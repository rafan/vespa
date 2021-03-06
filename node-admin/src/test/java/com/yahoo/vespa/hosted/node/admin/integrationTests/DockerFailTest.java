// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.integrationTests;

import com.yahoo.config.provision.NodeType;
import com.yahoo.vespa.hosted.dockerapi.ContainerName;
import com.yahoo.vespa.hosted.dockerapi.ContainerResources;
import com.yahoo.vespa.hosted.dockerapi.DockerImage;
import com.yahoo.vespa.hosted.node.admin.configserver.noderepository.NodeSpec;
import com.yahoo.vespa.hosted.provision.Node;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author freva
 */
public class DockerFailTest {

    @Test
    public void dockerFailTest() {
        try (DockerTester tester = new DockerTester()) {
            final DockerImage dockerImage = new DockerImage("dockerImage");
            final ContainerName containerName = new ContainerName("host1");
            final String hostname = "host1.test.yahoo.com";
            tester.addChildNodeRepositoryNode(new NodeSpec.Builder()
                    .hostname(hostname)
                    .wantedDockerImage(dockerImage)
                    .currentDockerImage(dockerImage)
                    .state(Node.State.active)
                    .nodeType(NodeType.tenant)
                    .flavor("docker")
                    .wantedRestartGeneration(1L)
                    .currentRestartGeneration(1L)
                    .minCpuCores(1)
                    .minMainMemoryAvailableGb(1)
                    .minDiskAvailableGb(1)
                    .build());

            tester.inOrder(tester.docker).createContainerCommand(
                    eq(dockerImage), eq(ContainerResources.from(1, 1)), eq(containerName), eq(hostname));
            tester.inOrder(tester.docker).executeInContainerAsUser(
                    eq(containerName), eq("root"), any(), eq(DockerTester.NODE_PROGRAM), eq("resume"));

            tester.docker.deleteContainer(new ContainerName("host1"));

            tester.inOrder(tester.docker).deleteContainer(eq(containerName));
            tester.inOrder(tester.docker).createContainerCommand(
                    eq(dockerImage), eq(ContainerResources.from(1, 1)), eq(containerName), eq(hostname));
            tester.inOrder(tester.docker).executeInContainerAsUser(
                    eq(containerName), eq("root"), any(), eq(DockerTester.NODE_PROGRAM), eq("resume"));

            verify(tester.nodeRepository, never()).updateNodeAttributes(any(), any());
        }
    }
}
