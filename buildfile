# Generated by Buildr 1.3.5, change to your liking
# Version number for this release
VERSION_NUMBER = "1.0.0"
# Group identifier for your projects
GROUP = "scala-servlet"
COPYRIGHT = ""

require 'buildr/scala'

repositories.remote << "http://www.ibiblio.org/maven2/"
repositories.remote << "http://download.java.net/maven/1/"
repositories.remote << "http://download.java.net/maven/2/"

SERVLET = 'javax.servlet:servlet-api:jar:2.5'
JERSEY_CORE_SERVER = 'com.sun.jersey:jersey-server:jar:1.0.3'
JERSEY_CORE_CLIENT = 'com.sun.jersey:jersey-client:jar:1.0.3'
JERSEY_CORE = 'com.sun.jersey:jersey-core:jar:1.0.3'
JERSEY_JSON = 'com.sun.jersey:jersey-json:jar:1.0.3'
ASM = 'asm:asm:jar:3.1'
JSR311_API = 'javax.ws.rs:jsr311-api:jar:1.0'
LIB_FOLDER = Dir['lib/*.jar']

# Fetch java mpd (not in the maven repositories)
JAVA_MPD_VERSION = '3.3'
JAVA_MPD = "java-mpd:java-mpd:jar:#{JAVA_MPD_VERSION}"
download artifact(JAVA_MPD) => "http://javampd.googlecode.com/files/javampd-#{JAVA_MPD_VERSION}.jar"

JERSEY_LIBS = [JERSEY_CORE_SERVER, JERSEY_CORE_CLIENT, JERSEY_CORE, JERSEY_JSON, JSR311_API, ASM]

task :deploy => ["mpctouch:deploy"]

desc "mpctouch: an mpd webapp client"
define "mpctouch" do

    project.version = VERSION_NUMBER
    project.group = GROUP
    manifest["Implementation-Vendor"] = COPYRIGHT
    compile.with SERVLET, JERSEY_LIBS, JAVA_MPD
    compile.using :deprecation => true, :debug => false, :optimise => true

    package(:war).libs -= artifacts(SERVLET)

    # Include html and js files and package into the root of the war
    package(:war).include _('src/ressources/*')

    task :deploy => :package do
        File.move('target/mpctouch-1.0.0.war', 'target/mpctouch.war')
    end

    task :deps do
      puts 'compile.dependencies: ', compile.dependencies
    end

end

# vim: set ft=ruby :
