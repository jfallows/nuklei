/*
 * Copyright 2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaazing.nuklei.tcp.internal.state;

import static java.util.EnumSet.allOf;
import static org.kaazing.nuklei.tcp.internal.state.BindingState.BINDING;
import static org.kaazing.nuklei.tcp.internal.state.BindingState.BOUND;
import static org.kaazing.nuklei.tcp.internal.state.BindingState.END;
import static org.kaazing.nuklei.tcp.internal.state.BindingState.START;
import static org.kaazing.nuklei.tcp.internal.state.BindingState.UNBINDING;
import static org.kaazing.nuklei.tcp.internal.state.BindingTransition.RECEIVED_BIND;
import static org.kaazing.nuklei.tcp.internal.state.BindingTransition.RECEIVED_UNBIND;
import static org.kaazing.nuklei.tcp.internal.state.BindingTransition.SENT_BOUND;
import static org.kaazing.nuklei.tcp.internal.state.BindingTransition.SENT_ERROR;
import static org.kaazing.nuklei.tcp.internal.state.BindingTransition.SENT_UNBOUND;

import org.kaazing.nuklei.tcp.internal.types.control.BindRO;
import org.kaazing.nuklei.tcp.internal.types.control.BoundRW;
import org.kaazing.nuklei.tcp.internal.types.control.ErrorRW;
import org.kaazing.nuklei.tcp.internal.types.control.UnbindRO;
import org.kaazing.nuklei.tcp.internal.types.control.UnboundRW;

public final class BindingStateMachine
{
    private final BindingHooks bindingHooks;

    public BindingStateMachine(
        BindingHooks bindingHooks)
    {
        this.bindingHooks = bindingHooks;
    }

    public void start(
        Binding binding)
    {
        binding.state = START;
        bindingHooks.whenInitialized.accept(binding);
    }

    public void received(
        Binding binding,
        BindRO bind)
    {
        transition(binding, RECEIVED_BIND);
        bindingHooks.whenBindReceived.accept(binding, bind);
    }

    public void received(
            Binding binding,
            UnbindRO unbind)
    {
        transition(binding, RECEIVED_UNBIND);
        bindingHooks.whenUnbindReceived.accept(binding, unbind);
    }

    public void sent(
            Binding binding,
            BoundRW bound)
    {
        transition(binding, SENT_BOUND);
        bindingHooks.whenBoundSent.accept(binding, bound);
    }

    public void sent(
            Binding binding,
            UnboundRW unbound)
    {
        transition(binding, SENT_UNBOUND);
        bindingHooks.whenUnboundSent.accept(binding, unbound);
    }

    public void sent(
            Binding binding,
            ErrorRW error)
    {
        transition(binding, SENT_ERROR);
        bindingHooks.whenErrorSent.accept(binding, error);
    }

    private void transition(Binding binding, BindingTransition transition)
    {
        binding.state = STATE_MACHINE[binding.state.ordinal()][transition.ordinal()];
    }

    private static final BindingState[][] STATE_MACHINE;

    static
    {
        int stateCount = BindingState.values().length;
        int transitionCount = BindingTransition.values().length;

        BindingState[][] stateMachine = new BindingState[stateCount][transitionCount];
        for (BindingState state : allOf(BindingState.class))
        {
            // default transition to "end" state
            for (BindingTransition transition : allOf(BindingTransition.class))
            {
                stateMachine[state.ordinal()][transition.ordinal()] = END;
            }
        }

        stateMachine[START.ordinal()][RECEIVED_BIND.ordinal()] = BINDING;
        stateMachine[BINDING.ordinal()][SENT_BOUND.ordinal()] = BOUND;
        stateMachine[BINDING.ordinal()][SENT_ERROR.ordinal()] = END;
        stateMachine[BOUND.ordinal()][RECEIVED_UNBIND.ordinal()] = UNBINDING;
        stateMachine[UNBINDING.ordinal()][SENT_UNBOUND.ordinal()] = END;
        stateMachine[UNBINDING.ordinal()][SENT_ERROR.ordinal()] = END;

        STATE_MACHINE = stateMachine;
    }
}
