package com.mattstine.dddworkshop.pizzashop.delivery;

import java.util.List;
import java.util.function.BiFunction;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * @author Matt Stine
 */
@Value
public final class DeliveryOrder implements Aggregate {

	@NonNull DeliveryOrderRef ref;

	@NonNull KitchenOrderRef kitchenOrderRef;

	@NonNull OnlineOrderRef onlineOrderRef;

	@Singular List<Pizza> pizzas;

	@NonNull EventLog $eventLog;

	@NonFinal
	State state;

	@Builder
	public DeliveryOrder(@NonNull DeliveryOrderRef ref,
	                      @NonNull KitchenOrderRef kitchenOrderRef,
	                      @NonNull OnlineOrderRef onlineOrderRef,
	                      @Singular List<Pizza> pizzas,
	                      @NonNull EventLog eventLog) {
		this.$eventLog = eventLog;
		this.ref = ref;
		this.kitchenOrderRef = kitchenOrderRef;
		this.onlineOrderRef = onlineOrderRef;
		this.pizzas = pizzas;
		this.state = State.READY_FOR_DELIVERY;
	}

	@SuppressWarnings("unused")
	private DeliveryOrder() {
		this.$eventLog = null;
		this.ref = null;
		this.kitchenOrderRef = null;
		this.onlineOrderRef = null;
		this.pizzas = null;
	}

	@Override
	public DeliveryOrder identity() {
		return DeliveryOrder.builder().
				eventLog(InProcessEventLog.IDENTITY).
				kitchenOrderRef(KitchenOrderRef.IDENTITY).
				onlineOrderRef(OnlineOrderRef.IDENTITY).
				ref(DeliveryOrderRef.IDENTITY).
				build();
	}

	@Override
	public BiFunction<DeliveryOrder, DeliveryOrderEvent, DeliveryOrder> accumulatorFunction() {
		return (order, evt) -> {
			if (evt instanceof DeliveryOrderAddedEvent) {
				DeliveryOrderAddedEvent doae = (DeliveryOrderAddedEvent) evt;
				return DeliveryOrder.builder().
						ref(doae.getRef()).
						kitchenOrderRef(doae.getState().getKitchenOrderRef()).
						eventLog(InProcessEventLog.instance()).
						onlineOrderRef(doae.getState().getOnlineOrderRef()).
						pizzas(doae.getState().getPizzas()).
						build();
			}
			throw new IllegalArgumentException("Unsupported DeliveryEventType:" + evt.getClass());
		};
	}

	@Override
	public Ref getRef() {
		return ref;
	}

	@Override
	public OrderState state() {
		return OrderState.builder().
				kitchenOrderRef(kitchenOrderRef).
				onlineOrderRef(onlineOrderRef).
				ref(ref).
				pizzas(pizzas).
				build();
	}

	boolean isReadyForDelivery() {
		return this.state == State.READY_FOR_DELIVERY;
	}

	enum State {
		READY_FOR_DELIVERY;
	}

	private static class Accumulator implements BiFunction<DeliveryOrder, DeliveryOrderEvent, DeliveryOrder> {

		@Override
		public DeliveryOrder apply(DeliveryOrder deliveryOrder, DeliveryOrderEvent deliveryOrderEvent) {
			return null;
		}
	}

	/*
	 * Pizza Value Object for KitchenOrder Details Only
	 */
	@Value
	public static final class Pizza {
		Size size;

		@Builder
		private Pizza(@NonNull Size size) {
			this.size = size;
		}

		public enum Size {
			SMALL, MEDIUM, LARGE
		}
	}

	@Value @Builder
	static class OrderState implements AggregateState {
		@NonNull DeliveryOrderRef ref;
		@NonNull KitchenOrderRef kitchenOrderRef;
		@NonNull OnlineOrderRef onlineOrderRef;
		@Singular List<Pizza> pizzas;
	}
}
