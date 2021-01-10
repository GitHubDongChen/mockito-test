package uyun.ant.lss.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

  @InjectMocks
  private ModuleService moduleService;

  @Nested
  @DisplayName("心跳中模块保存")
  class SaveTest {

    @Test
    @DisplayName("模块新增0-1")
    void shouldAddModuleWhen0To1() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module mockModule = new MockModule(mockAgent);

      /*打桩*/
      mockStoreModule(mockAgent);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule(mockModule));

      /*断言*/
      assertModuleSave(mockModule);
    }
    @Test
    @DisplayName("模块新增1-2")
    void shouldAddModuleWhen1To2() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module module1 = new MockModule(mockAgent);
      Module module2 = new MockModule(mockAgent);

      /*打桩*/
      mockStoreModule(mockAgent, module1);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule(module1, module2));

      /*断言*/
      assertModuleSave(module2);
    }

    @Test
    @DisplayName("模块减少2-1")
    void shouldDeleteModule() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module module1 = new MockModule(mockAgent);
      Module module2 = new MockModule(mockAgent);

      /*打桩*/
      mockStoreModule(mockAgent, module1, module2);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule(module1));

      /*断言*/
      assertModuleDelete(module2);
    }

    @Test
    @DisplayName("只删除stopped和started状态的模块")
    void shouldDeleteWhenStateMatch() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module stoppedModule = new MockModule(mockAgent);
      stoppedModule.setState(State.stopped);
      Module startedModule = new MockModule(mockAgent);
      startedModule.setState(State.started);
      Module otherStateModule = new MockModule(mockAgent);
      otherStateModule.setState(State.unknown);

      /*打桩*/
      mockStoreModule(mockAgent, stoppedModule, startedModule, otherStateModule);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule());

      /*断言*/
      assertModuleDelete(stoppedModule, startedModule);
    }

    @Test
    @DisplayName("只删除uninstalling状态的模块，配合gateway的作业处理")
    void shouldDeleteWhenStateIsUninstalling() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module uninstallingModule = new MockModule(mockAgent);
      uninstallingModule.setState(State.uninstalling);

      /*打桩*/
      mockStoreModule(mockAgent, uninstallingModule);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule());

      /*断言*/
      assertModuleDelete(uninstallingModule);
    }

    @Test
    @DisplayName("只删没有状态的模块")
    void shouldDeleteWhenStateIsNull() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module nullStateModule = new MockModule(mockAgent);
      nullStateModule.setState(null);

      /*打桩*/
      mockStoreModule(mockAgent, nullStateModule);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule());

      /*断言*/
      assertModuleDelete(nullStateModule);
    }

    @Test
    @DisplayName("模块不变")
    void nothingToDo() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module mockModule = new MockModule(mockAgent);

      /*打桩*/
      mockStoreModule(mockAgent, mockModule);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule(mockModule));

      /*断言*/
      assertModuleSave();
      assertModuleDelete();
    }

    @Test
    @DisplayName("模块不变，模块版本更新")
    void shouldUpdateVersion() {
      /*准备数据*/
      Agent mockAgent = new MockAgent();
      Module storeModule = new MockModule(mockAgent);
      Module heartbeatModule = new Module();
      BeanUtils.copyProperties(storeModule, heartbeatModule);
      heartbeatModule.setVersion("99.99.99");

      /*打桩*/
      mockStoreModule(mockAgent, storeModule);

      /*测试*/
      moduleService.save(mockAgent, prepareHeartbeatModule(heartbeatModule));

      /*断言*/
      assertModuleSave(heartbeatModule);
    }

    private void assertModuleSave(Module...modules) {
      if (modules.length == 0) {
        verify(moduleDao, times(0)).save(any());
        return;
      }
      for (Module module : modules) {
        if (module.getId() == null) {
          verify(moduleDao).save(argThat(new ModuleOfKeyMatcher(module)));
          log.debug("verify moduleDao save by id:{}", module);
        } else {
          verify(moduleDao).save(argThat(new OfIdMatcher<>(module)));
          log.debug("verify moduleDao save by key:{}", module);
        }
      }
    }

    private void assertModuleDelete(Module...modules) {
      if (modules.length == 0) {
        verify(moduleDao, times(0)).save(any());
        return;
      }
      for (Module module : modules) {
        verify(moduleDao).delete(argThat(new OfIdMatcher<>(module)));
        log.debug("verify moduleDao delete:{}", module);
      }
    }

    private List<Module> mockStoreModule(Agent agent, Module...modules) {
      when(agentDao.get(any(Agent.class))).thenReturn(agent);
      List<Module> moduleList = Arrays.asList(modules);
      when(moduleDao.list(any(Agent.class))).thenReturn(moduleList);
      return moduleList;
    }

    private List<Module> prepareHeartbeatModule(Module...modules) {
      for (Module module : modules) {
        module.setId(null);
        module.setName(null);
        module.setTenantId(null);
        module.setOuterObjectId(null);
        module.setState(null);
      }
      return Arrays.asList(modules);
    }

  }

}