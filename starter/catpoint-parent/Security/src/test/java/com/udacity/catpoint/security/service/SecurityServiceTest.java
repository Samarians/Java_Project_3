package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.image.FakeImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    @ParameterizedTest
    @MethodSource("provideSensorActivationTestCases")
    @DisplayName("1st and 2nd requirement: Sensor activation scenarios for different arming statuses")
    void handleSensorActivated_AlarmArmedAndSensorActivated_AlarmStatusIsChanged(ArmingStatus armingStatus,
                                                                                     AlarmStatus initialAlarmStatus,
                                                                                     AlarmStatus expectedAlarmStatus) {
        Sensor sensor_1 = new Sensor("Sensor 1", SensorType.DOOR);

        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(initialAlarmStatus);

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
    @DisplayName("When system is disarm, activate sensor won't change alarm status")
    void handleSensorActivated_SensorIsActivatedWhileSystemIsDisarm_AlarmStatusWontChange() {
        Sensor sensor_1 = new Sensor("Sensor 1", SensorType.DOOR);

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor_1, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }



    @Test
    @DisplayName("3rd requirement: If pending alarm and all sensors are inactive, return to no alarm state.")
    void handleSensorDeactivated_AllSensorsAreDeactivatedWhileSystemIsPendingAlarm_SystemReturnsToNoAlarm() {
        Sensor sensor = new Sensor("Test Sensor", SensorType.DOOR);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        sensor.setActive(true);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @MethodSource("provideSensorDeactivationTestCases")
    @DisplayName("4th requirement: If alarm is active, change in sensor state should not affect the alarm state.")
    void handleSensorDeactivated_senSorStatusChangedWhileAlarmIsActive_AlarmStatusShouldNotChange(Sensor sensor_1,
                                                                                                     Sensor sensor_2,
                                                                                                     AlarmStatus initialStatus) {

        sensor_1.setActive(true);
        sensor_2.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(initialStatus);

        securityService.changeSensorActivationStatus(sensor_1, false);
        securityService.changeSensorActivationStatus(sensor_2, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    private static Stream<Arguments> provideSensorDeactivationTestCases() {
        return Stream.of(
                arguments(new Sensor("Sensor 1", SensorType.DOOR), new Sensor("Sensor 2", SensorType.WINDOW), AlarmStatus.ALARM),
                arguments(new Sensor("Sensor 1", SensorType.DOOR), new Sensor("Sensor 2", SensorType.WINDOW), AlarmStatus.NO_ALARM)
        );
    }

    @Test
    @DisplayName("5th requirement: If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    void changeSensorActivationStatus_SensorActivatedWhileAlreadyActiveAndPendingAlarm_alarmStatusUpdatedToAlarm() {
        Sensor sensor_1 = new Sensor("Sensor 1", SensorType.DOOR);
        sensor_1.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor_1, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @MethodSource("alarmStatusProvider")
    @DisplayName("6th requirement: If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    void changeSensorActivationStatus_SensorDeactivatedWhileAlreadyInactive_noChangesToAlarmState(AlarmStatus initialStatus) {
        Sensor sensor = new Sensor("Test Sensor", SensorType.DOOR);

        when(securityRepository.getAlarmStatus()).thenReturn(initialStatus);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }
    private static Stream<Arguments> alarmStatusProvider() {
        return Stream.of(
                arguments(AlarmStatus.NO_ALARM),
                arguments(AlarmStatus.ALARM)
        );
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
    void catDetected_noCatDetectedAndAllSensorsInactive_systemNotChangedToNoAlarmState() {
        //when(securityRepository.getSensors()).thenReturn(createSensors(false, false));
        Sensor sensor_1 = new Sensor("Sensor 1", SensorType.DOOR);
        Sensor sensor_2 = new Sensor("Sensor 2", SensorType.WINDOW);

        Set<Sensor> listSensors = new HashSet<>();
        sensor_1.setActive(true);
        sensor_2.setActive(true);
        listSensors.add(sensor_1);
        listSensors.add(sensor_2);

        when(securityRepository.getSensors()).thenReturn(listSensors);
        lenient().when(securityRepository.getAlarmStatus()). thenReturn(AlarmStatus.ALARM);
        when(imageService.imageContainsCat(any(BufferedImage.class), eq(50.0f))).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("8th requirement: If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    void catDetected_noCatDetectedAndNotSensorsInactive_systemChangedToNoAlarmState() {
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
    }

    @ParameterizedTest
    @MethodSource("isCatDetectedProvider")
    @DisplayName("11th requirement: If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void catDetected_systemIsArmedHomeWhileCameraShowsCat_alarmStatusSetToAlarm(boolean isCatDetected, ArmingStatus setStatus) {
        lenient().when(securityRepository.getArmingStatus()).thenReturn(setStatus);
        when(imageService.imageContainsCat(any(BufferedImage.class), eq(50.0f))).thenReturn(isCatDetected);
        securityService.processImage(mock(BufferedImage.class));

        if (isCatDetected && setStatus == ArmingStatus.ARMED_HOME) {
            verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        } else {verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);};
    }

    private static  Stream<Arguments> isCatDetectedProvider() {
        return Stream.of(
        arguments(true, ArmingStatus.ARMED_HOME),
        arguments(true, ArmingStatus.DISARMED),
        arguments(true, ArmingStatus.ARMED_AWAY),
        arguments(false, ArmingStatus.DISARMED)
        );
    }
}

