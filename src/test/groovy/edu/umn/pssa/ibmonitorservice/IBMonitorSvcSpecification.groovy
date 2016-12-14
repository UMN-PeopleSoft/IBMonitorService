package edu.umn.pssa.ibmonitorservice

import spock.lang.Specification

class IBMonitorSvcSpecification extends Specification {

    def 'starting in debug mode returns a startup status of false'() {
        given:
            def it = new IBMonitorSvc()
        when:
            def success = it.monitorStartup()
        then:
            success == false
    }

}
