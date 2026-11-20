package net.aoba.utils.player;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class MiningControllerTest {
	private final MiningController.Actions actions = mock(MiningController.Actions.class);
	private final MiningController controller = new MiningController(actions);
	private final BlockPos first = new BlockPos(1, 64, 1);
	private final BlockPos second = new BlockPos(2, 64, 2);

	@Test
	void survivalMiningStartsOnceThenContinuesSameTarget() {
		when(actions.continueMining(first, Direction.UP)).thenReturn(true);

		controller.mine(first, Direction.UP, false, 0, 100);
		controller.mine(first, Direction.UP, false, 0, 101);

		InOrder order = inOrder(actions);
		order.verify(actions).start(first, Direction.UP);
		order.verify(actions).swing();
		order.verify(actions).continueMining(first, Direction.UP);
		order.verify(actions).swing();
		order.verify(actions, never()).stop();
	}

	@Test
	void switchingTargetStopsPreviousMiningBeforeStarting() {
		controller.mine(first, Direction.UP, false, 0, 100);
		controller.mine(second, Direction.NORTH, false, 0, 101);

		InOrder order = inOrder(actions);
		order.verify(actions).start(first, Direction.UP);
		order.verify(actions).stop();
		order.verify(actions).start(second, Direction.NORTH);
	}

	@Test
	void delaySuppressesActionsUntilElapsed() {
		controller.mine(first, Direction.UP, false, 50, 100);
		controller.mine(first, Direction.UP, false, 50, 120);

		verify(actions, never()).continueMining(first, Direction.UP);

		when(actions.continueMining(first, Direction.UP)).thenReturn(true);
		controller.mine(first, Direction.UP, false, 50, 150);
		verify(actions).continueMining(first, Direction.UP);
	}

	@Test
	void immediateMiningSendsStartThenStop() {
		controller.mine(first, Direction.UP, true, 0, 100);

		InOrder order = inOrder(actions);
		order.verify(actions).start(first, Direction.UP);
		order.verify(actions).swing();
		order.verify(actions).stop();
	}

	@Test
	void resetStopsOnlyAnActiveSurvivalTarget() {
		controller.reset();
		verify(actions, never()).stop();

		controller.mine(first, Direction.UP, false, 0, 100);
		controller.reset();
		verify(actions).stop();
	}
}
