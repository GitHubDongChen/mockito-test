package uyun.ant.lss.service;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uyun.ant.lss.dao.AgentDao;
import uyun.ant.lss.dao.entity.OfIdMatcher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentServiceTest {

  @InjectMocks
  private AgentService agentService;
  @Mock
  private AgentDao agentDao;

  @Nested
  @DisplayName("获取所有上级")
  class ListParentAgentMethodTest {
    @Test
    @DisplayName("根据agentId找不到Agent，返回空列表")
    void shouldReturnEmptyListWhenCantFindAgent() {
      /*测试*/
      List<Agent> agents = agentService.listParent(randomAlphabetic(24), MOCK_TENANT_ID);

      /*断言*/
      assertThat(agents).isEmpty();
    }

    @Test
    @DisplayName("根据agentId找不到OS，返回空列表")
    void shouldReturnEmptyListWhenCantFindOs() {
      /*准备数据*/
      Agent mockAgent = createMockAgent();

      /*测试*/
      List<Agent> agents = agentService.listParent(mockAgent);

      /*断言*/
      assertThat(agents).isEmpty();
    }

    @Test
    @DisplayName("根据agentId找到OS，但是supervisor为空，返回空列表")
    void shouldReturnEmptyListWhenSupervisorEmpty() {
      /*准备数据*/
      Agent mockAgent = createMockAgent();
      Os mockOs = createMockOs(mockAgent, null);

      /*测试*/
      List<Agent> agents = agentService.listParent(mockAgent);

      /*断言*/
      assertThat(agents).isEmpty();
    }

    @Test
    @DisplayName("根据agentId找到OS，但是agent和supervisor相同，返回空列表，容错处理")
    void shouldReturnEmptyListWhenAgentEqualSupervisor() {
      /*准备数据*/
      Agent mockAgent = createMockAgent();
      Os mockOs = createMockOs(mockAgent, mockAgent);

      /*测试*/
      List<Agent> agents = agentService.listParent(mockAgent);

      /*断言*/
      assertThat(agents).isEmpty();
    }

    @Test
    @DisplayName("Agent存在2个上级，返回上级列表")
    void shouldReturnParentAgentListWhenExistParent() {
      /*准备数据*/
      Agent level1Agent = createMockAgent();
      Os level1Os = createMockOs(level1Agent, null);
      Agent level2Agent = createMockAgent();
      Os level2Os = createMockOs(level2Agent, level1Agent);
      Agent level3Agent = createMockAgent();
      Os level3Os = createMockOs(level3Agent, level2Agent);

      /*打桩*/

      /*测试*/
      List<Agent> agents = agentService.listParent(level3Agent);

      /*断言*/
      assertThat(agents).hasSize(2);
    }

    @Test
    @DisplayName("Agent存在1个上级且上级agent和supervisor相同，容错处理")
    void shouldReturnParentAgentListWhenParentAgentEqualSupervisor() {
      /*准备数据*/
      Agent level1Agent = createMockAgent();
      Os level1Os = createMockOs(level1Agent, level1Agent);
      Agent level2Agent = createMockAgent();
      Os level2Os = createMockOs(level2Agent, level1Agent);

      /*打桩*/

      /*测试*/
      List<Agent> agents = agentService.listParent(level2Agent);

      /*断言*/
      assertThat(agents).hasSize(1);
    }
  }

  private Agent createMockAgent() {
    Agent agent = new MockAgent();

    // 打桩
    when(agentDao.get(argThat(new OfIdMatcher<>(agent)))).thenReturn(agent);
    when(agentDao.get(argThat(new AgentOfKeyMatcher(agent)))).thenReturn(agent);

    return agent;
  }

  private Os createMockOs(Agent agent, Agent supervisor) {
    Objects.requireNonNull(agent);
    Os os = new MockOs();
    os.setAgent(agent.getId());
    if (supervisor != null) {
      os.setSupervisor(supervisor.getId());
    }

    // 打桩
    when(osDao.get(argThat(new OfIdMatcher<>(agent)))).thenReturn(os);
    when(osDao.get(argThat(new AgentOfKeyMatcher(agent)))).thenReturn(os);

    return os;
  }


}