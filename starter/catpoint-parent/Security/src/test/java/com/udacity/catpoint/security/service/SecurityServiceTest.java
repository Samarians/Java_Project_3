package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.image.FakeImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private FakeImageService imageService;

    private SecurityService securityService;
    @BeforeAll
    static void setGlobalSensor () {
        System.out.println("Setting a global sensor");
    }

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    @ParameterizedTest
    @MethodSource("provideSensorActivationTestCases")
    @DisplayName("Parameterized Test: Sensor activation scenarios for different alarm statuses")
    void changeSensorActivationStatus_AlarmArmedAndSensorActivated_ChangeAlarmStatus(ArmingStatus armingStatus,
                                                                                     AlarmStatus initialAlarmStatus,
                                                                                     AlarmStatus expectedAlarmStatus) {
        Sensor sensor_1 = new Sensor("Sensor 1", SensorType.DOOR);

        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(initialAlarmStatus);

        securityService.changeSensorActivationStatus(sensor_1, true);

        verify(securityRepository).setAlarmStatus(expectedAlarmStatus);
    }

    private static Stream<Arguments> provideSensorActivationTestCases() {
        return Stream.of(
                arguments(ArmingStatus.ARMED_AWAY, AlarmStatus.NO_ALARM, AlarmStatus.PENDING_ALARM),
                arguments(ArmingStatus.ARMED_HOME, AlarmStatus.PENDING_ALARM, AlarmStatus.ALARM)
        );
    }

    @Test
    @DisplayName("3rd requirement: If pending alarm and all sensors are inactive, return to no alarm state.")
    void handleSensorDeactivated_pendingAlarmAndAllSensorsInactive_systemInNoAlarmState() {
        //Mock repository to save sensor status
        Sensor globalSensor_1 = new Sensor("Global Sensor 1", SensorType.DOOR);
        Sensor globalSensor_2 = new Sensor("Global Sensor 2", SensorType.WINDOW);

        Set<Sensor> listSensors = new HashSet<>();
        globalSensor_1.setActive(false);
        globalSensor_2.setActive(true);
        listSensors.add(globalSensor_1);


        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(listSensors);

        securityService.changeSensorActivationStatus(globalSensor_2, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("4th requirement: If alarm is active, change in sensor state should not affect the alarm state.")
    void handleSensorDeactivated_AlarmIsAlreadyActiveAndSensorStatusChanged_AlarmStatusDoesNotChange() {
        Sensor globalSensor_1 = new Sensor("Global Sensor 1", SensorType.DOOR);
        globalSensor_1.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(globalSensor_1, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    @DisplayName("5th requirement: If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    void changeSensorActivationStatus_SensorActivatedWhileAlreadyActiveAndPendingAlarm_alarmStatusUpdatedToAlarm() {
        Sensor globalSensor_1 = new Sensor("Global Sensor 1", SensorType.DOOR);
        globalSensor_1.setActive(true);

        //when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(globalSensor_1, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("6th requirement: If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    void changeSensorActivationStatus_SensorDeactivatedWhileAlreadyInactive_noChangesToAlarmState() {
        Sensor sensor = new Sensor("Test Sensor", SensorType.DOOR);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    @DisplayName("7th requirement: If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
    void catDetected_testCatDetectedWhileArmedHome_systemInAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), eq(50.0f))).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("8th requirement: If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    void catDetected_noCatDetectedAndSensorsInactive_systemChangedToNoAlarmState() {
        //when(securityRepository.getSensors()).thenReturn(createSensors(false, false));
        Sensor globalSensor_1 = new Sensor("Global Sensor 1", SensorType.DOOR);
        Sensor globalSensor_2 = new Sensor("Global Sensor 2", SensorType.WINDOW);

        Set<Sensor> listSensors = new HashSet<>();
        globalSensor_1.setActive(false);
        globalSensor_2.setActive(false);
        listSensors.add(globalSensor_1);
        listSensors.add(globalSensor_2);

        when(securityRepository.getSensors()).thenReturn(listSensors);
        when(imageService.imageContainsCat(any(BufferedImage.class), eq(50.0f))).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("9th requirement: If the system is disarmed, set the status to no alarm.")
    void setArmingStatus_systemDisarmed_setNoAlarmState() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("10th requirement: If the system is armed, reset all sensors to inactive.")
    void setArmingStatus_systemArmed_resetAllSensorsToInactive() {
        Sensor globalSensor_1 = new Sensor("Global Sensor 1", SensorType.DOOR);
        Sensor globalSensor_2 = new Sensor("Global Sensor 2", SensorType.WINDOW);

        Set<Sensor> listSensors = new HashSet<>();
        globalSensor_1.setActive(true);
        globalSensor_2.setActive(true);
        listSensors.add(globalSensor_1);
        listSensors.add(globalSensor_2);

        when(securityRepository.getSensors()).thenReturn(listSensors);

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        listSensors.forEach(sensor -> assertFalse(sensor.getActive()));

        // Verify that the arming status is set correctly in the repository
        //verify(securityRepository).setArmingStatus(ArmingStatus.ARMED_AWAY);
        //verify(securityRepository).updateSensor(globalSensor_1);
    }

    @Test
    @DisplayName("11th requirement: If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void catDetected_systemIsArmedHomeWhileCameraShowsCat_alarmStatusSetToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), eq(50.0f))).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}
