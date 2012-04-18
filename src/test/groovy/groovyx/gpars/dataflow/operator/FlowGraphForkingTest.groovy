// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup

import static groovyx.gpars.dataflow.Dataflow.task

class FlowGraphForkingTest extends GroovyTestCase {

    FlowGraph createFlowGraphInstance() {
        return new FlowGraph()
    }

    public void testOneForkOperator() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowVariable d = new DataflowVariable()
        final DataflowQueue e = new DataflowQueue()

        FlowGraph fGraph = createFlowGraphInstance()

        def op = fGraph.operator([a, b, c], [d, e], 1) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        task {
            fGraph.incrementWaitCount()
            sleep(500)
            a << 5
            fGraph.decrementWaitCount()
        }

        task {
            fGraph.incrementWaitCount()
            sleep(500)
            b << 20
            fGraph.decrementWaitCount()
        }

        task {
            fGraph.incrementWaitCount()
            sleep(500)
            c << 40
            fGraph.decrementWaitCount()
        }

        fGraph.waitForAll()

        assert d.isBound()
        assert 65 == d.val
        assert e.isBound()
        assert 4000 == e.val

    }

    public void testInverseOneForkOperator() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowVariable d = new DataflowVariable()
        final DataflowQueue e = new DataflowQueue()

        FlowGraph fGraph = createFlowGraphInstance()

        def op = fGraph.operator([a, b, c], [d, e], 1) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        task {
            sleep(500)
            a << 5
        }

        task {
            sleep(500)
            b << 20
        }

        task {
            sleep(500)
            c << 40
        }

        fGraph.waitForAll()

        assert !d.isBound()
        assert !e.isBound()

    }

    public void testTwoForkOperator() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowVariable d = new DataflowVariable()
        final DataflowQueue e = new DataflowQueue()

        FlowGraph fGraph = createFlowGraphInstance()

        def op = fGraph.operator([a, b, c], [d, e], 2) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        a << 5
        b << 20
        c << 40

        fGraph.waitForAll()

        assert d.isBound()
        assert 65 == d.val
        assert e.isBound()
        assert 4000 == e.val

    }

    public void testParallelism() {
        100.times {
            performParallelismTest(5, 4)
            performParallelismTest(5, 5)
            performParallelismTest(3, 5)
        }
    }

    private void performParallelismTest(int poolSize, forks) {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()
        final DefaultPGroup group = new DefaultPGroup(poolSize)

        final int parties = Math.min(poolSize - 1, forks)

        FlowGraph fGraph = new FlowGraph()

        def op = fGraph.operator([a, b, c], [d, e], forks) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, Thread.currentThread().name.hashCode()
        }

        a << 5

        b << 10

        [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16].each {c << it}

        fGraph.waitForAll()

        def results = (1..16).collect {d.val}
        assert 16 == results.size()
        assert results.containsAll([16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31])

        def threads = (1..16).collect {e.val}
        assert 16 == threads.size()

    }
}
