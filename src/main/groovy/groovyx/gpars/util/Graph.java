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

package groovyx.gpars.util;

import groovyx.gpars.actor.AbstractLoopingActor;
import groovyx.gpars.dataflow.operator.DataflowProcessor;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Graph {
    AbstractLoopingActor [] actors;
    volatile int numActiveCores;
    ReentrantLock lock= new ReentrantLock();;
    Condition condVar=lock.newCondition();
    public Graph(List<DataflowProcessor> processors){
        actors = new AbstractLoopingActor[processors.size()];
        for(int i=0; i < processors.size(); i++){
            this.actors[i] =  processors.get(i).getActor();
            this.actors[i].getCore().setConditionVariable(this);

        }
        startOperators(processors);

    }

    private void startOperators(final List<DataflowProcessor> processors) {
        for(int i=0;i<processors.size();i++){
            processors.get(i).start();
        }
    }

    private boolean isActorActive(AbstractLoopingActor actor){
        return actor.getCore().isActive();
    }


    private void terminate(){
        for(int i=0; i< actors.length; i++){
            actors[i].terminate();
        }
    }

    public void waitForAll(){

//        getActiveActors();
        lock.lock();
        try{
            while(numActiveCores >0){
                try {
                    condVar.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }
        }
        finally{
            lock.unlock();
        }
        terminate();
    }

    private void getActiveActors() {
        lock.lock();
        numActiveCores=0;
        for(int i=0;i<actors.length;i++){
            if(actors[i].getCore().isActive())numActiveCores++;
        }
        lock.unlock();
    }

    public void updateCounter(final int i) {
        lock.lock();
        numActiveCores+=i;
        System.out.println("num of actors "+numActiveCores);
        condVar.signal();
        lock.unlock();
    }
}
