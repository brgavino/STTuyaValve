
/*
 *  Tuya Zigbee Valve
 * 
 *  Copyright 2020 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 */
public static String version() { return "v0.0.1.20210727" }


private getMODEL_MAP() { 
    [
        'TS0601' : 3
    ]
}

metadata {
    definition(name: "C Smartfoss Tester", namespace: "CSF Test", author: "Kotsos", ocfDeviceType: "oic.d.watervalve", vid: "generic-valve") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Health Check"
        capability "Valve"
  		capability "Battery"
  		capability "Switch"
        

		fingerprint profileId: "0104", model: "TS0601", manufacturer: "_TZE200_akjefhj5", endpointId: "01", inClusters: "0000,0004,0005,EF00", outClusters: "0019,000A", application: "55", deviceJoinName: "Smartfoss"

      }
      
        preferences {
       // input name: "autoOff", type: "bool", title: "Turn off automatically", defaultValue: false, required: false
		input name: "autoofftimeinput", type: "number", title: "Auto off time period (minutes)", defaultValue: 10, required: false

    }
      
      tiles(scale: 2) {  

        
          
      main(["battery"])	//main(["mode","button"])
      details(["battery"])    
      
      }
 }
 
	


// Parse incoming device messages to generate events
def parse(String description) {
    Map map = [:]
    //def event = zigbee.getEvent(description)

    if (description?.startsWith('catchall:')) {
  		log.debug "description is $description"      
        log.debug description
        // call parseCatchAllMessage to parse the catchall message received
        map = parseCatchAllMessage(description)
        if (map != [:]) {
            log.debug "ok send event: $map.name : $map.value"
            sendEvent(name: map.name, value: map.value)
        }
    }
    else {
        log.warn "DID NOT PARSE MESSAGE for description : $description"
    }
}



// Commands from Smartthings
def close() {
    log.debug "called closed"
    zigbee.command(0xEF00, 0x0, "00010101000100")
}

def autoofftime(value) {
	String autoofftime = "00010B020004" + zigbee.convertToHexString((value * 60) as Integer, 8)
 }

def open() {
    log.debug "called open"
//    sendHubCommand(zigbee.command(0xEF00, 0x0, "00010B02000400015180"))
   
   def AOTI = autoofftimeinput
   log.debug "AOTI = ${AOTI}"        
    sendHubCommand(zigbee.command(0xEF00, 0x0, autoofftime(AOTI)))    
    zigbee.command(0xEF00, 0x0, "00010101000101")
}

def off() {
    close()
}

def on() {
    open()
}



def refresh() {
    log.debug "called refresh"
    zigbee.command(0xEF00, 0x0, "00020100")

}

def update() {
	
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.onOffRefresh() + zigbee.levelRefresh()
}




// Commands from the device

private Map parseCatchAllMessage(String description) {
    // Create a map from the raw zigbee message to make parsing more intuitive
    def msg = zigbee.parse(description)
    Map result = [:]
    switch(msg.clusterId) {
        case 0xEF00: 
            def attribute = getAttribute(msg.data)
            def value = getAttributeValue(msg.data)
         //   def AOTS = autoofftimeinput
            def autoofftimeFD = autoofftime(autoofftimeinput)
            
            switch (attribute) {      
                             
                case "valve": 
                    switch(value) {
                        case 0:
                            result = [
                                name: 'valve',
                                value: 'closed',
                                data: [buttonNumber: 1],
                                descriptionText: "$device.displayName button was pressed",
                                isStateChange: true
                            ]
                        sendEvent(name: 'switch',  value:"off", displayed: true )
                        break;

                        case 1:
                            result = [
                                name: 'valve',
                                value: 'open',
                                data: [buttonNumber: 1],
                                descriptionText: "$device.displayName button was pressed",
                                isStateChange: true
                            ]
						sendEvent(name: 'switch',  value:"on", displayed: true )
                        sendHubCommand(zigbee.command(0xEF00, 0x0, autoofftimeFD))
                        break;
                    }
                
                break;
  
            }
        
        break;
    }
    
    return result
}

private String getAttribute(ArrayList _data) {
    String retValue = ""
    if (_data.size() >= 5) {
        if (_data[2] == 1 && _data[3] == 1 && _data[4] == 0) {
            retValue = "valve"
        }
        else if (_data[2] == 2 && _data[3] == 2 && _data[4] == 0) {
            retValue = "level"
        }
    }
    
    return retValue
}

private int getAttributeValue(ArrayList _data) {
    int retValue = 0
    
    if (_data.size() >= 6) {
        int dataLength = _data[5] as Integer
        int power = 1;
        for (i in dataLength..1) {
            retValue = retValue + power * _data[i+5]
            power = power * 256
        }
    }
    
    return retValue
}
