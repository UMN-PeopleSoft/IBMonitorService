package edu.umn.pssa.ibmonitorservice

import spock.lang.Specification

class ConfigSpec extends Specification {

    Config config = new Config('configs.xml')

    def 'can access non-database configuration data'() {
        expect:
            config.getOnCallFile()    == '/oncall/file.name'
            config.getEmailReplyTo()  == 'dummy@bademail.com'
            config.getEmailUser()     == 'dummy@bademail.com'
            config.getEmailPassword() == 'passw0rd!'
            config.getEmailHost()     == 'localhost'
            config.getEmailPort()     == 25
            config.getDebugMode()     == 'ON'
    }

    def 'can access database configuration data'() {
        when:
            def databases = config.getDatabase()
            DatabaseType database = databases.first()
        then:
            databases.size()      == 1
            database.databaseName == 'DEBUG'
    }

}
