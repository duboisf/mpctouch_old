# Generated by Buildr 1.3.5, change to your liking
# Version number for this release
VERSION_NUMBER = "1.0.0"
# Group identifier for your projects
GROUP = "org"
COPYRIGHT = ""

require 'buildr/scala'

repositories.remote << "http://www.ibiblio.org/maven2"
repositories.remote << "http://mirrors.ibiblio.org/pub/mirrors/maven2"
repositories.remote << "http://download.java.net/maven/glassfish"
repositories.remote << "http://download.java.net/maven/1"
repositories.remote << "http://download.java.net/maven/2"

ASM = 'asm:asm:jar:3.1'

JSR311_API = 'javax.ws.rs:jsr311-api:jar:1.1.1'

SERVLET = 'javax.servlet:servlet-api:jar:2.5'

JERSEY_VERSION = '1.3'
JERSEY_CORE_SERVER = "com.sun.jersey:jersey-server:jar:#{JERSEY_VERSION}"
JERSEY_CORE = "com.sun.jersey:jersey-core:jar:#{JERSEY_VERSION}"
JERSEY_JSON = "com.sun.jersey:jersey-json:jar:#{JERSEY_VERSION}"
JERSEY = [JERSEY_CORE_SERVER, JERSEY_CORE, JERSEY_JSON, JSR311_API, ASM]

ATMOSPHERE_VERSION = '0.6.1'
ATMOSPHERE_JERSEY = transitive("org.atmosphere:atmosphere-jersey:jar:#{ATMOSPHERE_VERSION}")

# Fetch java mpd (not in the maven repositories)
JAVA_MPD_VERSION = '3.3'
JAVA_MPD = "org:javampd:jar:#{JAVA_MPD_VERSION}"
download artifact(JAVA_MPD) => "http://javampd.googlecode.com/files/javampd-#{JAVA_MPD_VERSION}.jar"

task :deploy => "mpctouch:webapp:deploy"

desc "mpctouch: an mpd webapp client"
define "mpctouch" do

    backend_project_name = ""
    project.version = VERSION_NUMBER
    project.group = GROUP
    manifest["Implementation-Vendor"] = COPYRIGHT

    desc "The mpctouch REST services"
    define "rest-services" do
        backend_project_name.replace project.name
        compile.with SERVLET, JERSEY, ATMOSPHERE_JERSEY, JAVA_MPD
        compile.using :deprecation => true, :debug => false, :optimise => true
        package :jar
    end

    desc "The mpctouch webapp"
    define "webapp" do
        # Remove servlet jar 'cause tomcat no like this jar (it provides its own)
        package(:war).libs -= artifacts(SERVLET)
        backend_project = project(backend_project_name)
        package(:war).with :libs => [backend_project, backend_project.compile.dependencies]

        # Rename to simpler filename
        task :deploy => :package do
            PROPER_NAME = project.name.gsub(':', '-')
            OLD_FILE = _("target/#{PROPER_NAME}-#{VERSION_NUMBER}.war")
            PROPER_PARENT_NAME = project.parent.name.gsub(':', '-')
            NEW_FILE = _("../#{PROPER_PARENT_NAME}.war")
            puts "Renaming " + OLD_FILE + " to " + NEW_FILE
            File.move(OLD_FILE, NEW_FILE)
        end
    end
end

# vim: set ft=ruby :
