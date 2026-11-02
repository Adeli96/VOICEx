#!/bin/sh
# VoiceX Gradle wrapper launcher — Made by CODEx Team
# Standard Gradle wrapper script. For the full canonical script, regenerate with:
#   gradle wrapper --gradle-version 8.7
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -n "$JAVA_HOME" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD="java"; fi
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
