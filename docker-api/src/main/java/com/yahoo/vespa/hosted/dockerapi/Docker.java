// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.dockerapi;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * API to simplify the com.github.dockerjava API for clients,
 * and to avoid OSGi exporting those classes.
 */
public interface Docker {

    interface CreateContainerCommand {
        CreateContainerCommand withLabel(String name, String value);
        CreateContainerCommand withEnvironment(String name, String value);

        /**
         * Mounts a directory on host inside the docker container.
         *
         * <p>Bind mount content will be <b>private</b> to this container (and host) only.
         *
         * <p>When using this method and selinux is enabled (/usr/sbin/sestatus), starting
         * multiple containers which mount host's /foo directory into the container, will make
         * /foo's content visible/readable/writable only inside the container which was last
         * started and on the host. All the other containers will get "Permission denied".
         *
         * <p>Use {@link #withSharedVolume(Path, Path)} to mount a given host directory
         * into multiple containers.
         */
        CreateContainerCommand withVolume(Path path, Path volumePath);

        /**
         * Mounts a directory on host inside the docker container.
         *
         * <p>The bind mount content will be <b>shared</b> among multiple containers.
         *
         * @see #withVolume(Path, Path)
         */
        CreateContainerCommand withSharedVolume(Path path, Path volumePath);
        CreateContainerCommand withNetworkMode(String mode);
        CreateContainerCommand withIpAddress(InetAddress address);
        CreateContainerCommand withUlimit(String name, int softLimit, int hardLimit);
        CreateContainerCommand withEntrypoint(String... entrypoint);
        CreateContainerCommand withManagedBy(String manager);
        CreateContainerCommand withAddCapability(String capabilityName);
        CreateContainerCommand withDropCapability(String capabilityName);
        CreateContainerCommand withPrivileged(boolean privileged);

        void create();
    }

    CreateContainerCommand createContainerCommand(
            DockerImage dockerImage,
            ContainerResources containerResources,
            ContainerName containerName,
            String hostName);

    Optional<ContainerStats> getContainerStats(ContainerName containerName);

    void startContainer(ContainerName containerName);

    void stopContainer(ContainerName containerName);

    void deleteContainer(ContainerName containerName);

    List<Container> getAllContainersManagedBy(String manager);

    Optional<Container> getContainer(ContainerName containerName);

    /**
     * Checks if the image is currently being pulled or is already pulled, if not, starts an async
     * pull of the image
     *
     * @param image Docker image to pull
     * @return true iff image being pulled, false otherwise
     */
    boolean pullImageAsyncIfNeeded(DockerImage image);

    /**
     * Deletes the local images that are currently not in use by any container and not recently used.
     */
    boolean deleteUnusedDockerImages(List<DockerImage> excludes, Duration minImageAgeToDelete);

    /**
     * @param containerName The name of the container
     * @param user can be "username", "username:group", "uid" or "uid:gid"
     * @param timeoutSeconds Timeout for the process to finish in seconds or without timeout if empty
     * @param command The command with arguments to run
     *
     * @return exitcodes, stdout and stderr in the ProcessResult
     */
    ProcessResult executeInContainerAsUser(ContainerName containerName, String user, OptionalLong timeoutSeconds, String... command);
}
