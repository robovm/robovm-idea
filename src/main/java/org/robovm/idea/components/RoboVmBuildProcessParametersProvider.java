package org.robovm.idea.components;

import com.google.gson.Gson;
import com.intellij.compiler.server.BuildProcessParametersProvider;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import groovy.lang.GroovyObject;
import org.apache.tools.ant.taskdefs.Ant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.slf4j.Logger;
import org.slf4j.impl.Log4jLoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by badlogic on 19/06/15.
 */
public class RoboVmBuildProcessParametersProvider extends BuildProcessParametersProvider {
    @Override
    @NotNull
    public List<String> getClassPath() {
        List<String> classpath = new ArrayList<>();
        classpath.add(PathUtil.getJarPathForClass(Ant.class));
        classpath.add(PathUtil.getJarPathForClass(GroovyObject.class));
        classpath.add(PathUtil.getJarPathForClass(Gson.class));
        classpath.add(PathUtil.getJarPathForClass(Logger.class));
        classpath.add(PathUtil.getJarPathForClass(Log4jLoggerFactory.class));
        return classpath;
    }
}
