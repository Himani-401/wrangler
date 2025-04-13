/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.wrangler.registry;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import io.cdap.cdap.api.artifact.ArtifactId;
import io.cdap.cdap.api.artifact.ArtifactInfo;
import io.cdap.cdap.api.artifact.ArtifactManager;
import io.cdap.cdap.api.artifact.ArtifactSummary;
import io.cdap.cdap.api.artifact.ArtifactVersion;
import io.cdap.cdap.api.artifact.CloseableClassLoader;
import io.cdap.cdap.api.plugin.PluginClass;
import io.cdap.cdap.api.plugin.PluginConfigurer;
import io.cdap.cdap.api.plugin.PluginProperties;
import io.cdap.cdap.api.service.http.HttpServiceContext;
import io.cdap.cdap.etl.api.StageContext;
import io.cdap.cdap.etl.api.Transform;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveLoadException;
import io.cdap.wrangler.utils.ArtifactSummaryComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.Nullable;

/**
 * A User Executor Registry in a collection of user defined directives.
 */
public final class UserDirectiveRegistry implements DirectiveRegistry {
  private static final String WRANGLER_TRANSFORM = "wrangler-transform";
  private static final String WRANGLER_PLUGIN = "Wrangler";

  private final Map<String, Map<String, DirectiveInfo>> registry = new ConcurrentSkipListMap<>();
  private final List<CloseableClassLoader> classLoaders = new ArrayList<>();
  private StageContext context;
  private HttpServiceContext manager;
  private ArtifactSummary wranglerArtifact;

  /**
   * Constructor for service context.
   */
  public UserDirectiveRegistry(HttpServiceContext manager) {
    this.manager = manager;
  }

  /**
   * Constructor for transform context.
   */
  public UserDirectiveRegistry(StageContext context) {
    this.context = context;
  }

  @Override
  public DirectiveInfo get(String namespace, String name) throws DirectiveLoadException {
    DirectiveInfo directiveInfo = registry.getOrDefault(namespace, Collections.emptyMap()).get(name);
    if (directiveInfo != null) {
      return directiveInfo;
    }

    Class<? extends Directive> directive;
    try {
      directive = getDirective(namespace, name);
      if (directive == null) {
        throw new DirectiveLoadException(
          String.format("10-5 - Unable to load the user defined directive '%s'. " +
                          "Please check if the artifact containing UDD is still present.", name)
        );
      }
      return DirectiveInfo.fromUser(directive, null);
    } catch (IllegalArgumentException e) {
      throw new DirectiveLoadException(
        String.format("Directive '%s' not found. Check if the directive is spelled correctly or artifact " +
                        "containing the directive has been uploaded or you might be missing " +
                        "'#pragma load-directives %s;'", name, name), e
      );
    } catch (Exception e) {
      throw new DirectiveLoadException(e.getMessage(), e);
    }
  }

  @Nullable
  private Class<? extends Directive> getDirective(String namespace, String name) throws IOException {
    if (context != null) {
      return context.loadPluginClass(name);
    }
    PluginConfigurer configurer = manager.createPluginConfigurer(namespace);
    return configurer.usePluginClass(Directive.TYPE, name, UUID.randomUUID().toString(),
                                     PluginProperties.builder().build());
  }

  @Override
  public void reload(String namespace) throws DirectiveLoadException {
    Map<String, DirectiveInfo> newRegistry = new TreeMap<>();
    Map<String, DirectiveInfo> currentRegistry = registry.computeIfAbsent(namespace,
                                                                          k -> new ConcurrentSkipListMap<>());

    ArtifactManager artifactManager = getArtifactManager();
    if (artifactManager != null) {
      try {
        List<ArtifactInfo> artifacts = artifactManager.listArtifacts(namespace);
        ArtifactSummary latestWrangler = null;
        for (ArtifactInfo artifact : artifacts) {
          boolean isWranglerArtifact = artifact.getName().equalsIgnoreCase(WRANGLER_TRANSFORM);
          Set<PluginClass> plugins = artifact.getClasses().getPlugins();
          CloseableClassLoader artifactClassLoader = null;

          for (PluginClass plugin : plugins) {
            if (Directive.TYPE.equalsIgnoreCase(plugin.getType())) {
              if (artifactClassLoader == null) {
                artifactClassLoader = artifactManager.createClassLoader(namespace, artifact,
                                                                        getClass().getClassLoader());
                classLoaders.add(artifactClassLoader);
              }

              Class<?> cls = artifactClassLoader.loadClass(plugin.getClassName());
              if (!Directive.class.isAssignableFrom(cls)) {
                throw new DirectiveLoadException("Plugin class " + plugin.getClassName() + " does not implement the "
                                                   + Directive.class.getName() + " interface");
              }
              DirectiveInfo info = DirectiveInfo.fromUser((Class<? extends Directive>) cls,
                                                          new ArtifactId(artifact.getName(),
                                                                         new ArtifactVersion(artifact.getVersion()),
                                                                         artifact.getScope()));
              newRegistry.put(info.name(), info);
            }

            if (isWranglerArtifact && WRANGLER_PLUGIN.equals(plugin.getName())
                && Transform.PLUGIN_TYPE.equals(plugin.getType())) {
              latestWrangler = Optional.ofNullable(latestWrangler)
                .map(l -> ArtifactSummaryComparator.pickLatest(l, artifact))
                .orElse(artifact);
            }
          }
        }

        if (latestWrangler != null) {
          wranglerArtifact = latestWrangler;
        }

        MapDifference<String, DirectiveInfo> difference = Maps.difference(currentRegistry, newRegistry);

        // Remove old
        for (String directive : difference.entriesOnlyOnLeft().keySet()) {
          currentRegistry.remove(directive);
        }

        // Update common
        for (String directive : difference.entriesInCommon().keySet()) {
          currentRegistry.put(directive, difference.entriesInCommon().get(directive));
        }

        // Add new
        for (String directive : difference.entriesOnlyOnRight().keySet()) {
          currentRegistry.put(directive, difference.entriesOnlyOnRight().get(directive));
        }
      } catch (IllegalAccessException | InstantiationException | IOException | ClassNotFoundException e) {
        throw new DirectiveLoadException(e.getMessage(), e);
      }
    }
  }

  @Nullable
  private ArtifactManager getArtifactManager() {
    return manager;
  }

  @Nullable
  @Override
  public ArtifactSummary getLatestWranglerArtifact() {
    return wranglerArtifact;
  }

  @Override
  public Iterable<DirectiveInfo> list(String namespace) {
    Map<String, DirectiveInfo> namespaceDirectives = registry.getOrDefault(namespace, Collections.emptyMap());
    return namespaceDirectives.values();
  }

  @Override
  public void close() throws IOException {
    for (CloseableClassLoader classLoader : classLoaders) {
      classLoader.close();
    }
  }
}
