package uyun.ant.lss.service;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uyun.ant.lss.TestCommonConstants.MOCK_TENANT_ID;

import java.io.Serializable;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uyun.ant.lss.dao.AgentDao;
import uyun.ant.lss.dao.NetworkDomainDao;
import uyun.ant.lss.dao.OsDao;
import uyun.ant.lss.dao.entity.MockAgentDO;
import uyun.ant.lss.dao.entity.MockNetworkDomain;
import uyun.ant.lss.dao.entity.NetworkDomain;
import uyun.ant.lss.dao.entity.Os;
import uyun.ant.lss.service.entity.InfoWrapper.NetworkDomainAccessor;
import uyun.ant.lss.service.entity.InfoWrapper.AgentAccessor;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NetworkDomainServiceTest {

  @InjectMocks
  private NetworkDomainService networkDomainService;

  @Mock
  private AgentDao agentDao;
  @Mock
  private OsDao osDao;
  @Mock
  private NetworkDomainDao networkDomainDao;

  @Test
  @DisplayName("Info中存在网络域且网络域为资源ID")
  void networkDomainIdIsRight() {
    /*准备数据*/
    NetworkDomain networkDomain = prepareNetworkDomain();

    /*打桩*/
    NetworkDomainAccessor networkDomainAccessor =
        mockNetworkDomainAccessor(networkDomain.getId(), networkDomain.getTenantId());

    /*测试*/
    networkDomainService.handleAgentBelongs(
        networkDomainAccessor,
        mockLevel1Agent()
    );

    /*断言*/
    verifyNetworkDomainSetTo(networkDomain.getId(), networkDomainAccessor);
  }

  @Test
  @DisplayName("Info中存在网络域且网络域为网络域Code")
  void networkDomainCodeIsRight() {
    /*准备数据*/
    NetworkDomain networkDomain = prepareNetworkDomain();

    /*打桩*/
    NetworkDomainAccessor networkDomainAccessor =
        mockNetworkDomainAccessor(networkDomain.getCode(), networkDomain.getTenantId());

    /*测试*/
    networkDomainService.handleAgentBelongs(
        networkDomainAccessor,
        mockLevel1Agent()
    );

    /*断言*/
    verifyNetworkDomainSetTo(networkDomain.getId(), networkDomainAccessor);
  }

  @Test
  @DisplayName("Info中存在网络域且不是ID也不是code")
  void shouldThrowExWhenNetworkDomainWrong() {
    /*打桩*/
    NetworkDomainAccessor networkDomainAccessor =
        mockNetworkDomainAccessor(randomAlphabetic(5), MOCK_TENANT_ID);

    /*测试*/
    Assertions.assertThrows(Exception.class, () -> {
      networkDomainService.handleAgentBelongs(networkDomainAccessor, mockLevel1Agent());
    });
  }

  @Test
  @DisplayName("Info中不存在网络域一级代理设置为默认域")
  void shouldSetDefaultNetworkDomainWhenAgentIsLevel1() {
    /*准备数据*/
    NetworkDomain defaultNetworkDomain = prepareDefaultNetworkDomain();

    /*打桩*/
    // 网络域为空
    NetworkDomainAccessor networkDomainAccessor =
        mockNetworkDomainAccessor(null, MOCK_TENANT_ID);

    /*测试*/
    networkDomainService.handleAgentBelongs(networkDomainAccessor, mockLevel1Agent());

    /*断言*/
    verifyNetworkDomainSetTo(defaultNetworkDomain.getId(), networkDomainAccessor);
  }

  private void verifyNetworkDomainSetTo(String networkDomainId, NetworkDomainAccessor accessor) {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(accessor).overrideNetworkDomain(captor.capture());
    assertThat(captor.getValue()).isEqualTo(networkDomainId);
  }

  @Test
  @DisplayName("Info中不存在网络域二级代理设置为上级代理的网络域")
  void shouldSetParentNetworkDomainWhenAgentIsLevel2() {
    /*准备数据*/
    NetworkDomain parentNetworkDomain = mockParentNetworkDomain();

    /*打桩*/
    // 不存在网络域
    NetworkDomainAccessor networkDomainAccessor = mockNetworkDomainAccessor(null, MOCK_TENANT_ID);

    /*测试*/
    networkDomainService.handleAgentBelongs(
        networkDomainAccessor,
        mockLevel2Agent(randomAlphabetic(32))
    );

    /*断言*/
    verifyNetworkDomainSetTo(parentNetworkDomain.getId(), networkDomainAccessor);
  }

  @Test
  @DisplayName("Info中不存在网络域且上级代理网络找不到")
  void shouldThrowExWhenNoParentNetworkDomain() {
    /*打桩*/
    NetworkDomainAccessor networkDomainAccessor = mockNetworkDomainAccessor(null);

    /*测试*/
    Assertions.assertThrows(Exception.class, () -> {
      networkDomainService.handleAgentBelongs(
          networkDomainAccessor,
          mockLevel2Agent(randomAlphabetic(32))
      );
    });
  }

  private NetworkDomain mockParentNetworkDomain() {
    NetworkDomain networkDomain = prepareNetworkDomain();
    when(agentDao.get(anyString(), anyString())).thenReturn(new MockAgentDO());
    Os os = new Os();
    os.setValueIfNotBlank("networkDomain", (Serializable) Collections.singletonMap("id", networkDomain.getId()));
    when(osDao.getByAgentId(anyString(), anyString())).thenReturn(os);
    return networkDomain;
  }

  private NetworkDomainAccessor mockNetworkDomainAccessor(String networkDomain) {
    return this.mockNetworkDomainAccessor(networkDomain, MOCK_TENANT_ID);
  }

  private NetworkDomainAccessor mockNetworkDomainAccessor(String networkDomain, String tenantId) {
    NetworkDomainAccessor mock = mock(NetworkDomainAccessor.class);
    if (StringUtils.isBlank(networkDomain)) {
      when(mock.existNetworkDomain()).thenReturn(false);
      when(mock.getNetworkDomain()).thenReturn(null);
    } else {
      when(mock.existNetworkDomain()).thenReturn(true);
      when(mock.getNetworkDomain()).thenReturn(networkDomain);
    }
    when(mock.getTenantId()).thenReturn(tenantId);
    return mock;
  }

  private AgentAccessor mockLevel1Agent() {
    AgentAccessor mock = mock(AgentAccessor.class);
    when(mock.existsParent()).thenReturn(true);
    return mock;
  }

  private AgentAccessor mockLevel2Agent(String parentAgentId) {
    AgentAccessor mock = mock(AgentAccessor.class);
    when(mock.existsParent()).thenReturn(false);
    when(mock.getParentAgentId()).thenReturn(parentAgentId);
    return mock;
  }

  private NetworkDomain prepareNetworkDomain() {
    NetworkDomain networkDomain = new MockNetworkDomain();
    mockNetworkDomainAction(networkDomain);
    return networkDomain;
  }

  private NetworkDomain prepareDefaultNetworkDomain() {
    NetworkDomain networkDomain = new MockNetworkDomain();
    networkDomain.setCode(NetworkDomain.DEFAULT_NETWORK_DOMAIN_CODE);
    mockNetworkDomainAction(networkDomain);
    return networkDomain;
  }

  public void mockNetworkDomainAction(NetworkDomain networkDomain) {
    when(networkDomainDao.get(argThat(argument -> {
      if (argument == null) {
        return false;
      }
      if (StringUtils.isAnyBlank(argument.getId(), argument.getTenantId())) {
        return false;
      }
      return argument.getId().equals(networkDomain.getId())
          && argument.getTenantId().equals(networkDomain.getTenantId());
    }))).thenReturn(networkDomain);
    when(networkDomainDao.get(argThat(argument -> {
      if (argument == null) {
        return false;
      }
      if (StringUtils.isAnyBlank(argument.getCode(), argument.getTenantId())) {
        return false;
      }
      return argument.getCode().equals(networkDomain.getCode())
          && argument.getTenantId().equals(networkDomain.getTenantId());
    }))).thenReturn(networkDomain);
  }


}