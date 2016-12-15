/*
 * Copyright 2016-present The Material Motion Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.motion.streams;

import android.util.Property;

import com.google.android.material.motion.observable.IndefiniteObservable.Subscription;
import com.google.android.material.motion.streams.MotionObservable.MotionObserver;
import com.google.android.material.motion.streams.MotionObservable.MotionState;
import com.google.android.material.motion.streams.MotionObservable.ScopedWritable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.android.material.motion.streams.MotionObservable.ACTIVE;
import static com.google.android.material.motion.streams.MotionObservable.AT_REST;

/**
 * A MotionAggregator writes the output of streams to properties and observes their overall state.
 */
public final class MotionAggregator {

  private final List<Subscription> subscriptions = new ArrayList<>();
  private final Set<MotionObserver<?>> activeObservers = new HashSet<>();
  @MotionState
  private int aggregateState;

  /**
   * Subscribes to the stream, writes its output to the given property, and observes its state.
   */
  public <O, T> void write(MotionObservable<T> stream, final O target, final Property<O, T> property) {
    subscriptions.add(stream.subscribe(new MotionObserver<T>() {

      @Override
      public void next(T value) {
        property.set(target, value);
      }

      @Override
      public void state(@MotionState int state) {
        onObserverStateChange(this, state);
      }
    }));
  }

  /**
   * Subscribes to the stream, writes its output to the given property, and observes its state.
   */
  public <T> void write(MotionObservable<T> stream, final ScopedWritable<T> property) {
    subscriptions.add(stream.subscribe(new MotionObserver<T>() {

      @Override
      public void next(T value) {
        property.write(value);
      }

      @Override
      public void state(@MotionState int state) {
        onObserverStateChange(this, state);
      }
    }));
  }

  private void onObserverStateChange(MotionObserver<?> observer, @MotionState int state) {
    boolean changed = false;

    switch (state) {
      case ACTIVE:
        changed = activeObservers.add(observer);
        break;
      case AT_REST:
        changed = activeObservers.remove(observer);
        break;
    }

    if (changed) {
      onAggregateStateChange(activeObservers.isEmpty() ? AT_REST : ACTIVE);
    }
  }

  private void onAggregateStateChange(@MotionState int aggregateState) {
    if (this.aggregateState != aggregateState) {
      this.aggregateState = aggregateState;
    }
  }
}
