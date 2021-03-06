package org.codeaholics.tools.build.pant;

/*
 *   Copyright 2010-2011 Danny Yates
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.Target;

public class DependencyGraph {
    private final Map<String, DependencyGraphEntry> dependencyGraphEntries = new HashMap<String, DependencyGraphEntry>();
    private final Map<String, Target> targets;
    private final List<String> prePhaseTargets;
    private final DependencyGraphEntryFactory dependencyGraphEntryFactory;

    public DependencyGraph(final Map<String, Target> targets, final List<String> prePhaseTargets,
                           final DependencyGraphEntryFactory dependencyGraphEntryFactory) {
        this.targets = targets;
        this.prePhaseTargets = prePhaseTargets;
        this.dependencyGraphEntryFactory = dependencyGraphEntryFactory;
    }

    public DependencyGraphEntry buildDependencies(final Target target) {
        final String targetName = target.getName();

        if (dependencyGraphEntries.containsKey(targetName)) {
            // already done/in progress
            return dependencyGraphEntries.get(targetName);
        }

        final boolean isPrePhase = prePhaseTargets.contains(targetName);

        final DependencyGraphEntry dependencyGraphEntry = dependencyGraphEntryFactory.create(target, isPrePhase);
        dependencyGraphEntries.put(targetName, dependencyGraphEntry);

        processDependencies(dependencyGraphEntry);

        return dependencyGraphEntry;
    }

    private void processDependencies(final DependencyGraphEntry dependencyGraphEntry) {
        @SuppressWarnings("unchecked")
        final Enumeration<String> dependencies = dependencyGraphEntry.getTarget().getDependencies();

        while (dependencies.hasMoreElements()) {
            final String dependency = dependencies.nextElement();

            dependencyGraphEntry.addPredecessor(dependency);
            final DependencyGraphEntry predecessorDependencyGraphEntry = buildDependencies(targets.get(dependency));
            predecessorDependencyGraphEntry.addSuccessor(dependencyGraphEntry.getTarget().getName());
        }
    }

    public List<DependencyGraphEntry> discoverAllSchedulableTargets() {
        final List<DependencyGraphEntry> schedulableTargets = new LinkedList<DependencyGraphEntry>();

        for (final DependencyGraphEntry dependencyGraphEntry: dependencyGraphEntries.values()) {
            if (dependencyGraphEntry.isTargetWaiting() && isReadyToSchedule(dependencyGraphEntry)) {
                schedulableTargets.add(dependencyGraphEntry);
            }
        }

        return schedulableTargets;
    }

    private boolean isReadyToSchedule(final DependencyGraphEntry dependencyGraphEntry) {
        for (final String predecessor: dependencyGraphEntry.getPredecessors()) {
            if (!dependencyGraphEntries.get(predecessor).isTargetComplete()) {
                return false;
            }
        }

        return true;
    }
}
