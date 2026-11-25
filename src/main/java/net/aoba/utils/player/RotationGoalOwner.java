package net.aoba.utils.player;

import net.aoba.Aoba;
import net.aoba.managers.rotation.goals.Goal;

/**
 * Tracks rotation ownership without clearing a newer goal installed by another
 * module. The rotation manager is shared by every module, so a boolean is not
 * enough to establish ownership.
 */
public final class RotationGoalOwner {
	interface GoalAccess {
		Goal<?> getGoal();

		void setGoal(Goal<?> goal);
	}

	private final GoalAccess goalAccess;
	private Goal<?> ownedGoal;

	public RotationGoalOwner() {
		this(new GoalAccess() {
			@Override
			public Goal<?> getGoal() {
				return Aoba.getInstance().rotationManager.getGoal();
			}

			@Override
			public void setGoal(Goal<?> goal) {
				Aoba.getInstance().rotationManager.setGoal(goal);
			}
		});
	}

	RotationGoalOwner(GoalAccess goalAccess) {
		this.goalAccess = goalAccess;
	}

	public boolean isActive() {
		return ownedGoal != null && goalAccess.getGoal() == ownedGoal;
	}

	public Goal<?> getOwnedGoal() {
		return ownedGoal;
	}

	public void set(Goal<?> goal) {
		ownedGoal = goal;
		goalAccess.setGoal(goal);
	}

	public void clear() {
		if (ownedGoal != null && goalAccess.getGoal() == ownedGoal) {
			goalAccess.setGoal(null);
		}
		ownedGoal = null;
	}
}
