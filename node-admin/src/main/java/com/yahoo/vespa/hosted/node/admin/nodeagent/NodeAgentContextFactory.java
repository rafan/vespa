// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.nodeagent;

import com.yahoo.vespa.flags.FlagSource;
import com.yahoo.vespa.hosted.node.admin.configserver.noderepository.NodeSpec;

/**
 * @author freva
 */
public interface NodeAgentContextFactory {
    NodeAgentContext create(NodeSpec nodeSpec, FlagSource flagSource);
}
