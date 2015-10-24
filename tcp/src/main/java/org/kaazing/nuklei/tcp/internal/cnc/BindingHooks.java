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

package org.kaazing.nuklei.tcp.internal.cnc;

import java.util.function.Consumer;

import org.kaazing.nuklei.tcp.internal.cnc.functions.BindingConsumer;
import org.kaazing.nuklei.tcp.internal.cnc.types.BindRO;
import org.kaazing.nuklei.tcp.internal.cnc.types.BoundRW;
import org.kaazing.nuklei.tcp.internal.cnc.types.ErrorRW;
import org.kaazing.nuklei.tcp.internal.cnc.types.UnbindRO;
import org.kaazing.nuklei.tcp.internal.cnc.types.UnboundRW;

public class BindingHooks
{
    public Consumer<Binding> whenInitialized = (b) -> {};

    public Consumer<Binding> whenError = (b) -> {};

    public BindingConsumer<BindRO> whenBindReceived = (b, d) -> {};

    public BindingConsumer<UnbindRO> whenUnbindReceived = (b, d) -> {};

    public BindingConsumer<BoundRW> whenBoundSent = (b, e) -> {};

    public BindingConsumer<UnboundRW> whenUnboundSent = (b, e) -> {};

    public BindingConsumer<ErrorRW> whenErrorSent = (b, e) -> {};
}
