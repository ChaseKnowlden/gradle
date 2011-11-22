/*
 * Copyright 2007-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.clientmodule

import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.apache.ivy.core.resolve.ResolveData
import org.gradle.api.artifacts.ClientModule
import org.gradle.api.internal.artifacts.ivyservice.GradleDependencyResolver
import spock.lang.Specification

/**
 * @author Hans Dockter
 */
class ClientModuleResolverTest extends Specification {
    final ModuleDescriptor module = Mock()
    final GradleDependencyResolver targetResolver = Mock()
    final ResolveData resolveData = Mock()
    final ClientModuleRegistry clientModuleRegistry = Mock()
    final ClientModuleResolver resolver = new ClientModuleResolver(clientModuleRegistry)

    def "resolves dependency descriptor that matches module in supplied registry"() {
        DependencyDescriptor dependencyDescriptor = dependency("module")
        when:

        clientModuleRegistry.getClientModule("module") >> module
        def resolvedDependency = resolver.getDependency(dependencyDescriptor, resolveData)

        then:
        resolvedDependency.descriptor == module
    }

    def "returns null for unknown module"() {
        DependencyDescriptor dependencyDescriptor = dependency(null)
        
        expect:
        resolver.getDependency(dependencyDescriptor, resolveData) == null   
    }
    
    def dependency(String module) {
        DependencyDescriptor descriptor = Mock()
        _ * descriptor.getExtraAttribute(ClientModule.CLIENT_MODULE_KEY) >> module
        return descriptor
    }
}
