package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @InjectMocks
    private SecurityService securityService;
    private Sensor sensor;

    private final String name = UUID.randomUUID().toString();
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private FakeImageService fakeImageService;
    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, fakeImageService);
        sensor = new Sensor(name, SensorType.DOOR);
    }

    //Test Case 01 :If alarm is armed and a sensor becomes activated,
    // put the system into pending alarm status.
    @Test
    @DisplayName(("TestCase01"))
    void alarmArmed_sensorActivated_alarmStatusPending() throws Exception {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    //Test Case 02 :If alarm is armed and a sensor becomes activated and the system is already pending alarm,
    // set the alarm status to alarm.
    @Test
    @DisplayName(("TestCase02"))
    void alarmArmed_sensorActivated_systemAlreadyPending_alarmStatusAlarm() throws Exception {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test Case 03 :If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    @DisplayName(("TestCase03"))
    void alarmPending_allSensorInactive_noAlarmState() throws Exception {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test Case 04 :If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    @DisplayName(("TestCase04"))
    void alarmActive_changeSensorState_notAffectAlarmState() throws Exception {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        // Compare data
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Test Case 05 :If a sensor is activated while already active and the system is in pending state,
    // change it to alarm state.
    @Test
    @DisplayName(("TestCase05"))
    void sensorActivatedAlreadyActive_systemPendingState_alarmStatusAlarm() throws Exception {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        // Compare data
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test Case 06 :If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    @DisplayName(("TestCase06"))
    void sensorDeactivatedAlreadyInactive_noChangeAlarmStatus() throws Exception {
        sensor.setActive(false);
        Mockito.doReturn(AlarmStatus.PENDING_ALARM).when(securityRepository).getAlarmStatus();
        securityService.changeSensorActivationStatus(sensor, true);
        // Compare data
        Mockito.verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        Mockito.verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Test Case 07 :If the image service identifies an image containing a cat while the system is armed-home,
    // put the system into alarm status.
    @Test
    @DisplayName(("TestCase07"))
    void imageServiceImageContainingCat_SystemArmedHome_alarmStatusAlarm() throws Exception {
        Mockito.doReturn(true).when(fakeImageService).imageContainsCat(Mockito.any(), Mockito.anyFloat());
        Mockito.doReturn(ArmingStatus.ARMED_HOME).when(securityRepository).getArmingStatus();
        BufferedImage bufferedImageMock = Mockito.mock(BufferedImage.class);
        securityService.processImage(bufferedImageMock);
        // Compare data
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test Case 08 :If the image service identifies an image that does not contain a cat,
    // change the status to no alarm as long as the sensors are not active.
    @Test
    @DisplayName(("TestCase08"))
    void imageServiceImageNotContainCat_alarmStatusNoAlarm_sensorNotActive() throws Exception {
        sensor.setActive(false);
        Mockito.doReturn(false).when(fakeImageService).imageContainsCat(Mockito.any(), Mockito.anyFloat());
        BufferedImage bufferedImageMock = Mockito.mock(BufferedImage.class);
        securityService.processImage(bufferedImageMock);
        // Compare data
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test Case 09 :If the system is disarmed, set the status to no alarm.
    @Test
    @DisplayName(("TestCase09"))
    void systemDisarmed_alarmStatusNoAlarm() throws Exception {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        // Compare data
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test Case 10 :If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @MethodSource("armingStatus")
    @DisplayName(("TestCase10"))
    void systemArmed_allSensorsInactive(ArmingStatus armingStatus) throws Exception {
        Set<Sensor> sensorSet = generateAlSensor();
        for (Sensor sensor : sensorSet) {
            sensor.setActive(true);
        }
        doReturn(AlarmStatus.PENDING_ALARM).when(securityRepository).getAlarmStatus();
        doReturn(sensorSet).when(securityRepository).getSensors();
        securityService.setArmingStatus(armingStatus);
        // Compare data
        for (Sensor sensor : sensorSet) {
            Assertions.assertFalse(sensor.getActive());
        }
    }

    //Test Case 11 :If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    @DisplayName(("TestCase11"))
    void systemArmedHome_cameraShowsCat_alarmStatusAlarm() throws Exception {
        Mockito.doReturn(true).when(fakeImageService).imageContainsCat(Mockito.any(), Mockito.anyFloat());
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        BufferedImage bufferedImageMock = Mockito.mock(BufferedImage.class);
        securityService.processImage(bufferedImageMock);
        // Compare data
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test Case 12 :If the system is alarm and SensorDeactivated, set the alarm status pending.
    @Test
    @DisplayName(("TestCase12"))
    void systemAlarm_sensorDeactivated_alarmStatusPending() throws Exception {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Test Case 14 :If the image service identifies an image that contain a cat  change the status to no alarm
    @Test
    @DisplayName(("TestCase14"))
    void imageServiceImageContainCat_alarmStatusAlarm() throws Exception {
        sensor.setActive(false);
        Mockito.doReturn(true).when(fakeImageService).imageContainsCat(Mockito.any(), Mockito.anyFloat());
        BufferedImage bufferedImageMock = Mockito.mock(BufferedImage.class);
        securityService.processImage(bufferedImageMock);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        // Compare data
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("addSensor")
    void addSensor() throws Exception {
        Sensor sensor = new Sensor("Sensor", SensorType.DOOR);
        securityService.addSensor(sensor);
    }

    @Test
    @DisplayName("removeSensor")
    void removeSensor() throws Exception {
        Sensor sensor = new Sensor("Sensor", SensorType.DOOR);
        securityService.removeSensor(sensor);

    }

    @Test
    @DisplayName("addStatusListener")
    void addStatusListener() throws Exception {
        securityService.addStatusListener(statusListener);
    }

    @Test
    @DisplayName("removeStatusListener")
    void removeStatusListener() throws Exception {
        securityService.removeStatusListener(statusListener);

    }

    static Set<Sensor> generateAlSensor() {
        Set<Sensor> listSensor = new HashSet<>();
        // create list sensor
        listSensor.add(new Sensor("Sensor_Door", SensorType.DOOR));
        listSensor.add(new Sensor("Sensor_Window", SensorType.WINDOW));
        listSensor.add(new Sensor("Sensor_Motion", SensorType.MOTION));
        return listSensor;
    }

    static Stream<Arguments> armingStatus() {
        return Stream.of(Arguments.of(ArmingStatus.ARMED_HOME),
                Arguments.of(ArmingStatus.ARMED_AWAY)
        );
    }
}
