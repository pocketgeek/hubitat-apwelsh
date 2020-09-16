/**
 * AdvancedHueGroup
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device for the Advanced Hue Bridge Integeration app.  This device is used to manage hue zones and rooms
 * as color lights.  The key difference between this and the built-in hueGroup device is that this device supports the
 * refresh capability -- if enabled -- to allow for fast device refresh, and to act as the parent device for the
 * AdvancedHueScene device.
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2020 Armand Peter Welsh
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
 **/

preferences {
    input name: "autoRefresh", type: "bool", defaultValue: false, title: "Auto Refresh",        description: "Should this device support automatic refresh" 
    if (autoRefresh)
        input name: "refreshInterval", type: "number", defaultValue: 60, title: "Refresh Inteval", description: "Number of seconds to refresh the group state" 
    input name: "anyOn",       type: "bool", defaultValue: true,  title: "ANY on or ALL on",    description: "When ebabled, the group is considered on when any light is on"
    input name: "logEnable",   type: "bool", defaultValue: false, title: "Enable debug logging"

}

metadata {
    definition (name:      "AdvancedHueGroup", 
                namespace: "apwelsh", 
                author:    "Armand Welsh", 
                importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-group.groovy") {
        
        capability "Light"
        capability "ChangeLevel"
        capability "Switch"
        capability "SwitchLevel"
        capability "Actuator"
        capability "ColorControl"
        capability "ColorMode"
        capability "ColorTemperature"
        capability "Refresh"
        
        command "activateScene", [[name: "Scene Identitier*", type: "STRING", description: "Enter a scene name or the scene number as defined by the Hue Bridge"]]
    }
}

def installed() {
    parent.getDeviceState(this)
}

def updated() {
    if (logEnable) log.debug "Preferences updated"
    parent.getDeviceState(this)
}

/** Switch Commands **/

def on() {
    parent.componentOn(this)
}

def off() {
    parent.componentOff(this)
}

/** ColorControl Commands **/

def setColor(colormap) {
    parent.componentSetColor(this, colormap)
}
def setHue(hue) {
    parent.componentSetHue(this, hue)
}

def setSaturation(saturation) {
    parent.componentSetSaturation(this, saturation)
}

/** ColorTemperature Commands **/

def setColorTemperature(colortemperature) {
    parent.componentSetColorTemperature(this, colortemperature)
}

/** SwitchLevel Commands **/

def setLevel(level, duration=null) {
    parent.componentSetLevel(this, level, duration)
}

/** ChangeLevel Commands **/

def startLevelChange(direction) {
    parent.componentStartLevelChange(this, direction)
}

def stopLevelChange() {
    parent.componentStopLevelChange(this)
}

/** Refresh Commands **/
def refresh() {
    parent.getDeviceState(this)
}

def resetRefreshSchedule() {
    unschedule()
    if (autoRefresh) schedule("0/${refreshInterval} * * * * ?", refresh) // Move the schedule to avoid redundant refresh events    
}


def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
}

def setHueProperty(name, value) {
    switch (name) {
        case "scene":
        def child = getChildDevice(networkIdForScene(value))
        child.sendEvent(name: "switch", value: "on")
        child.unschedule()
        child.runInMillis(400, "off")
        break;
        case "any_on":
        if (anyOn) sendEvent(name: "switch", value: value ? "on" : "off")
        break;
        case "all_on":
        if (!anyOn) sendEvent(name: "switch", value: value ? "on" : "off")
        break;
    }
}

def deviceIdNode(deviceNodeId) {
     parent.deviceIdNode(deviceNodeId)
}

def networkIdForScene(sceneId) {
    parent.networkIdForScene(deviceIdNode(device.deviceNetworkId), sceneId)
}

def activateScene(scene) {
    def sceneId = parent.findScene(deviceIdNode(device.deviceNetworkId), scene)?.key
    if (sceneId) {
        if (logEnable) log.info "Activate Scene: ${sceneId}"
        parent.setDeviceState(this, ["scene": sceneId])
    }
}

/*
 * Component Child Methods
 */

void componentOn(child) {
    def sceniId = deviceIdNode(child.deviceNetworkId)
    parent.setDeviceState(this, ["scene":sceniId])
}

void componentOff(child) {
    // Only change the state to off, there is not action to actually be performed.
    child.sendEvent(name: "switch", value: "off")
}

void componentRefresh(child){
    if (logEnable) log.info "received refresh request from ${child.displayName} - ignored"
}
