package uyun.ant.lss.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uyun.ant.lss.service.entity.ConnectEvent;
import uyun.ant.lss.service.entity.ConnectEvent.ConnectEventStateEnum;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ConnectEventConvergeServiceTest {

  @InjectMocks
  private ConnectEventConvergeService connectEventConvergeService;

  @Test
  @DisplayName("测试上报了一条断开事件用例1")
  void reportDisconnectedEventCase1() {
    /*准备数据*/
    ConnectEvent report = mockConnectEvent(ConnectEventStateEnum.disconnected, 2L, "ch1");

    /*测试*/
    ConnectEvent result = connectEventConvergeService.converge(Arrays.asList(report));

    /*断言*/
    assertThat(result).isEqualTo(report);
  }

  @Test
  @DisplayName("测试上报了一条断开事件用例2")
  void reportDisconnectedEventCase2() {
    /*准备数据*/
    ConnectEvent store = mockConnectEvent(ConnectEventStateEnum.connected, 1L, "ch1");
    ConnectEvent report = mockConnectEvent(ConnectEventStateEnum.disconnected, 2L, "ch1");

    /*测试*/
    ConnectEvent result = connectEventConvergeService.converge(Arrays.asList(store, report));

    /*断言*/
    assertThat(result).isEqualTo(report);
  }

  @Test
  @DisplayName("测试上报了一条连接事件")
  void reportConnectedEvent() {
    /*准备数据*/
    ConnectEvent store = mockConnectEvent(ConnectEventStateEnum.connected, 1L, "ch1");
    ConnectEvent report = mockConnectEvent(ConnectEventStateEnum.disconnected, 2L, "ch1");

    /*测试*/
    ConnectEvent result = connectEventConvergeService.converge(Arrays.asList(store, report));

    /*断言*/
    assertThat(result).isEqualTo(report);
  }


  @Test
  @DisplayName("测试上报了两条事件用例1")
  void report2EventCase1() {
    /*准备数据*/
    ConnectEvent store = mockConnectEvent(ConnectEventStateEnum.connected, 1L, "ch1");
    ConnectEvent report1 = mockConnectEvent(ConnectEventStateEnum.connected, 2L, "ch2");
    ConnectEvent report2 = mockConnectEvent(ConnectEventStateEnum.disconnected, 3L, "ch1");

    /*打桩*/

    /*测试*/
    ConnectEvent result = connectEventConvergeService.converge(Arrays.asList(store, report1, report2));

    /*断言*/
    assertThat(result).isEqualTo(report1);
  }

  @Test
  @DisplayName("测试上报了两条事件用例2")
  void report2EventCase2() {
    /*准备数据*/
    ConnectEvent store = mockConnectEvent(ConnectEventStateEnum.connected, 1L, "ch1");
    ConnectEvent report1 = mockConnectEvent(ConnectEventStateEnum.disconnected, 2L, "ch1");
    ConnectEvent report2 = mockConnectEvent(ConnectEventStateEnum.connected, 3L, "ch2");

    /*打桩*/

    /*测试*/
    ConnectEvent result = connectEventConvergeService.converge(Arrays.asList(store, report1, report2));

    /*断言*/
    assertThat(result).isEqualTo(report2);
  }

  private ConnectEvent mockConnectEvent(ConnectEventStateEnum state, long eventTime, String channel) {
    return new ConnectEvent(state.name(), eventTime, channel, null);
  }
}