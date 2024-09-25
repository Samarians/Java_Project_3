package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.image.FakeImageService;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 * <p>
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private FakeImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();

    public SecurityService(SecurityRepository securityRepository, FakeImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
//    This method satisfy the 9th requirement
//    9th: If the system is disarmed, set the status to no alarm.
//    This method does not satisfy the 10th requirement
//    10th: If the system is armed, reset all sensors to inactive.
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            //Reset all sensors to inactive when arming
            List<Sensor> sensors = new ArrayList<>(securityRepository.getSensors());
            sensors.forEach(sensor -> {
                sensor.setActive(false);
                securityRepository.updateSensor(sensor);
            });
        }
        securityRepository.setArmingStatus(armingStatus);

    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
//    This method satisfies the 7th and the 11th requirement
//    7th: If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
//    11th: If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
//    This method does not satisfy the 8th requirement
//    If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
//
    private void catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && allSensorsInactive()) {
            // Change status to NO_ALARM only if all sensors are inactive
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
//    This method satisfy the 1st and 2nd requirement:
//    1st: If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
//    2nd: If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.

    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
//    This one does not satisfy the 3rd and 4th requirement:
//    3rd: If pending alarm and all sensors are inactive, return to no alarm state.
//    4th: If alarm is active, change in sensor state should not affect the alarm state.

    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> {
                if ( allSensorsInactive()) {setAlarmStatus(AlarmStatus.NO_ALARM);}
            }
            case ALARM -> {}
//            setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Helper method to check if all sensors are inactive.
     * @return True if all sensors are inactive, false otherwise.
     */
    private boolean allSensorsInactive() {
        return securityRepository.getSensors().stream().noneMatch(Sensor::getActive);
    }


    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
//    This method does not satisfy the 5th requirement:
//    5th: If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if(!sensor.getActive() && active) {
            handleSensorActivated();
        } else if (sensor.getActive() && !active) {
            handleSensorDeactivated();
        } else if (sensor.getActive() && active && securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            // If the sensor is already active and reactivated in pending state, set to ALARM
            setAlarmStatus(AlarmStatus.ALARM);
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use it's provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
