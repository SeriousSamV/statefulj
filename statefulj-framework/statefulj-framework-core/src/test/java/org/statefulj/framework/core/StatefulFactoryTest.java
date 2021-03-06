/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.statefulj.framework.core;

import org.alternative.AltTestRepositoryFactoryBeanSupport;
import org.alternative.AltTestUserController;
import org.alternative.AltTestUserRepository;
import org.junit.Test;

import static org.junit.Assert.*;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.statefulj.framework.core.controllers.FailedMemoryController;
import org.statefulj.framework.core.controllers.MemoryController;
import org.statefulj.framework.core.controllers.NoRetryController;
import org.statefulj.framework.core.controllers.UserController;
import org.statefulj.framework.core.dao.UserRepository;
import org.statefulj.framework.core.mocks.MockBeanDefinitionRegistryImpl;
import org.statefulj.framework.core.mocks.MockProxy;
import org.statefulj.framework.core.mocks.MockRepositoryFactoryBeanSupport;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

public class StatefulFactoryTest {
	
	
	@Test
	public void testFSMConstruction() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		
		BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
		
		BeanDefinition userRepo = BeanDefinitionBuilder
				.genericBeanDefinition(MockRepositoryFactoryBeanSupport.class)
				.getBeanDefinition();
		userRepo.getPropertyValues().add("repositoryInterface", UserRepository.class.getName());

		registry.registerBeanDefinition("userRepo", userRepo);
	
		BeanDefinition userController = BeanDefinitionBuilder
				.genericBeanDefinition(UserController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("userController", userController);
	
		ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
		StatefulFactory factory = new StatefulFactory();
		
		factory.postProcessBeanDefinitionRegistry(registry);
		
		BeanDefinition userControllerMVCProxy = registry.getBeanDefinition(refFactory.getBinderId("mock"));
		
		assertNotNull(userControllerMVCProxy);
		
		Class<?> proxyClass = Class.forName(userControllerMVCProxy.getBeanClassName());
		
		assertNotNull(proxyClass);
		
		assertEquals(MockProxy.class, proxyClass);
		
		// Verify that FIVE_STATE is blocking
		//
		BeanDefinition stateFive = registry.getBeanDefinition(refFactory.getStateId(UserController.FIVE_STATE));
		
		assertEquals(true, stateFive.getConstructorArgumentValues().getArgumentValue(2, Boolean.class).getValue());
		
		BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
		assertNotNull(fsm);
		assertEquals(20, fsm.getConstructorArgumentValues().getArgumentValue(2, Integer.class).getValue());
		assertEquals(250, fsm.getConstructorArgumentValues().getArgumentValue(3, Integer.class).getValue());
	}
 
	@Test
	public void testFSMConstructionWithNonDefaultRetry() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		
		BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
		
		BeanDefinition userRepo = BeanDefinitionBuilder
				.genericBeanDefinition(MockRepositoryFactoryBeanSupport.class)
				.getBeanDefinition();
		userRepo.getPropertyValues().add("repositoryInterface", UserRepository.class.getName());

		registry.registerBeanDefinition("userRepo", userRepo);
	
		BeanDefinition noRetryController = BeanDefinitionBuilder
				.genericBeanDefinition(NoRetryController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("noRetryController", noRetryController);
	
		ReferenceFactory refFactory = new ReferenceFactoryImpl("noRetryController");

		StatefulFactory factory = new StatefulFactory();
		
		factory.postProcessBeanDefinitionRegistry(registry);

		BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
		assertNotNull(fsm);
		assertEquals(1, fsm.getConstructorArgumentValues().getArgumentValue(2, Integer.class).getValue());
		assertEquals(1, fsm.getConstructorArgumentValues().getArgumentValue(3, Integer.class).getValue());
	}
 
	@Test
	public void testAlternativePackages() throws ClassNotFoundException {
		BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
		
		BeanDefinition testUserRepo = BeanDefinitionBuilder
				.genericBeanDefinition(AltTestRepositoryFactoryBeanSupport.class)
				.getBeanDefinition();
		testUserRepo.getPropertyValues().add("repositoryInterface", AltTestUserRepository.class.getName());

		registry.registerBeanDefinition("testUserRepo", testUserRepo);
	
		BeanDefinition testUserController = BeanDefinitionBuilder
				.genericBeanDefinition(AltTestUserController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("testUserController", testUserController);
	
		ReferenceFactory refFactory = new ReferenceFactoryImpl("testUserController");
		StatefulFactory factory = new StatefulFactory("org.alternative");
		
		factory.postProcessBeanDefinitionRegistry(registry);
		
		BeanDefinition testUserControllerMVCProxy = registry.getBeanDefinition(refFactory.getBinderId("test"));
		
		assertNotNull(testUserControllerMVCProxy);
		
		Class<?> proxyClass = Class.forName(testUserControllerMVCProxy.getBeanClassName());
		
		assertNotNull(proxyClass);
		
		assertEquals(MockProxy.class, proxyClass);
		
		BeanDefinition stateOne = registry.getBeanDefinition(refFactory.getStateId(AltTestUserController.ONE_STATE));
		
		assertNotNull(stateOne);
		
		BeanDefinition persister = registry.getBeanDefinition(refFactory.getPersisterId());
		assertNotNull(persister);
	}

	@Test
	public void testMemoryPersistor() throws ClassNotFoundException {
		BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
		
		BeanDefinition memoryController = BeanDefinitionBuilder
				.genericBeanDefinition(MemoryController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("memoryController", memoryController);
	
		ReferenceFactory refFactory = new ReferenceFactoryImpl("memoryController");
		StatefulFactory factory = new StatefulFactory();
		
		factory.postProcessBeanDefinitionRegistry(registry);
		
		BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
		assertNotNull(fsm);

		BeanDefinition persister = registry.getBeanDefinition(refFactory.getPersisterId());
		assertNotNull(persister);
		assertEquals(MemoryPersisterImpl.class.getName(), persister.getBeanClassName());

		BeanDefinition harness = registry.getBeanDefinition(refFactory.getFSMHarnessId());
		assertNull(harness);
	}

	@Test(expected=RuntimeException.class)
	public void testMemoryFailurePersistor() throws ClassNotFoundException {
		BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
		
		BeanDefinition failedMemoryController = BeanDefinitionBuilder
				.genericBeanDefinition(FailedMemoryController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("failedMemoryController", failedMemoryController);
	
		StatefulFactory factory = new StatefulFactory();
		
		factory.postProcessBeanDefinitionRegistry(registry);
		
	}
}
