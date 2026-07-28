package net.aoba.utils.player;

import java.util.function.Predicate;

import net.aoba.Aoba;
import net.aoba.event.events.StartAttackEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class InteractionUtils {
	private static final Minecraft MC = Minecraft.getInstance();
	public static final int OFFHAND = 45;

	/**
	 * Raycasts towards a specific block at a specific vector.
	 * 
	 * @param targetBlock Block to raycast to.
	 * @param face        Face to check.
	 * @return BlockHitResult if a ray collision occured, null otherwise.
	 */
	public static BlockHitResult raycastBlock(BlockPos targetBlock, Direction face) {
		Vec3 eye = MC.player.getEyePosition();
		Vec3 end = eye.add(MC.player.getLookAngle().scale(MC.player.blockInteractionRange()));
		BlockHitResult ray = MC.level
				.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, MC.player));
		if (ray.getType() != HitResult.Type.BLOCK)
			return null;

		if (!targetBlock.equals(ray.getBlockPos()))
			return null;

		if (ray.getDirection() != face)
			return null;
		return ray;
	}

	public static EntityHitResult raycastEntity(Entity target, double reach) {
		return raycastEntity(entity -> entity == target, reach);
	}

	public static EntityHitResult raycastEntity(Predicate<Entity> filter, double reach) {
		Vec3 eye = MC.player.getEyePosition();
		Vec3 lookVector = MC.player.getViewVector(1.0f);
		Vec3 end = eye.add(lookVector.scale(reach));

		BlockHitResult blockHit = MC.level
				.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, MC.player));
		if (blockHit.getType() != HitResult.Type.MISS)
			end = blockHit.getLocation();

		AABB hitcastSearchBox = MC.player.getBoundingBox().expandTowards(lookVector.scale(reach)).inflate(1.0);
		return ProjectileUtil.getEntityHitResult(MC.player, eye, end, hitcastSearchBox, filter,
				eye.distanceToSqr(end));
	}

	/**
	 * Places a block at a position
	 * 
	 * @param blockPos  Position to place the block
	 * @param hand      Hand to use to place.
	 * @param swingHand Whether to swing the hand
	 * @return True if the block was placed, false otherwise.
	 */
	public static boolean placeBlock(BlockPos blockPos, InteractionHand hand, boolean swingHand) {
		Inventory inventory = MC.player.getInventory();
		ItemStack itemInHand;
		if (hand == InteractionHand.MAIN_HAND)
			itemInHand = inventory.getSelectedItem();
		else
			itemInHand = inventory.getItem(OFFHAND);

		if (itemInHand.getItem() instanceof BlockItem) {
			Direction side = getPlaceSide(blockPos);
			if (side == null)
				return false;

			BlockPos neighbour = blockPos.relative(side);
			Vec3 placePos = Vec3.atCenterOf(blockPos).add(side.getStepX() * 0.5, side.getStepY() * 0.5,
					side.getStepZ() * 0.5);
			BlockHitResult raytraceResult = new BlockHitResult(placePos, side.getOpposite(), neighbour, false);
			return interactBlock(raytraceResult, hand, swingHand);
		} else
			return false;
	}

	/**
	 * Interacts with a block.
	 * 
	 * @param blockHitResult Raycast result of the block.
	 * @param hand           Hand to use.
	 * @param swingHand      Whether to swing the players hand.
	 * @return True if the block was interacted with.
	 */
	public static boolean interactBlock(BlockHitResult blockHitResult, InteractionHand hand, boolean swingHand) {
		InteractionResult result = MC.gameMode.useItemOn(MC.player, hand, blockHitResult);
		if (result.consumesAction()) {
			if (swingHand)
				MC.player.swing(hand);
			else
				MC.getConnection().send(new ServerboundSwingPacket(hand));
			return true;
		} else
			return false;
	}

	/**
	 * Attacks an entity
	 * 
	 * @param entity Entity to attack.
	 */
	public static void attack(Entity entity) {
		// Fire onAttackStart event to simulate player attacking normally.
		// Allows us to inject actions between hits such as criticals, or shield
		// breaker.
		StartAttackEvent event = new StartAttackEvent(entity);
		Aoba.getInstance().eventManager.Fire(event);

		if (event.isCancelled()) {
			return;
		}

		MC.gameMode.attack(MC.player, entity);
		MC.player.swing(InteractionHand.MAIN_HAND);
	}

	public static Direction getPlaceSide(BlockPos blockPos) {
		Vec3 lookVec = Vec3.atCenterOf(blockPos).subtract(MC.player.getEyePosition());
		double bestRelevancy = -Double.MAX_VALUE;
		Direction bestSide = null;

		for (Direction side : Direction.values()) {
			BlockPos neighborPos = blockPos.relative(side);
			BlockState blockState = MC.level.getBlockState(neighborPos);

			if (blockState.isAir() || !blockState.getFluidState().isEmpty())
				continue;

			AxisDirection direction = side.getAxisDirection();
			double relevancy = side.getAxis().choose(lookVec.x(), lookVec.y(), lookVec.z()) * direction.getStep();
			if (relevancy > bestRelevancy) {
				bestRelevancy = relevancy;
				bestSide = side;
			}
		}

		return bestSide;
	}

	public static boolean isBehindWall(BlockPos blockPos) {
		Vec3 eyePos = MC.player.getEyePosition(1.0f);
		for (Direction face : Direction.values()) {
			Vec3 faceCenter = Vec3.atCenterOf(blockPos).add(face.getStepX() * 0.5, face.getStepY() * 0.5,
					face.getStepZ() * 0.5);
			BlockHitResult result = MC.level.clip(
					new ClipContext(eyePos, faceCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, MC.player));
			if (result.getType() != HitResult.Type.BLOCK || result.getBlockPos().equals(blockPos))
				return false;
		}
		return true;
	}

	public static boolean isBehindWall(Entity entity) {
		BlockPos entityPos = entity.blockPosition();
		Vec3 playerEyePos = MC.player.getEyePosition(1.0f);

		Vec3 toEntityVec = entity.position().subtract(playerEyePos);
		Vec3 extendedVec = playerEyePos.add(toEntityVec.scale(1.1));
		VoxelShape entityShape = Shapes.create(entity.getBoundingBox());

		BlockHitResult result = MC.level.clipWithInteractionOverride(playerEyePos, extendedVec, entityPos, entityShape,
				entity.getInBlockState());

		if (result != null && result.getType() == BlockHitResult.Type.BLOCK) {
			BlockState blockState = MC.level.getBlockState(result.getBlockPos());
			return blockState.canOcclude();
		}

		return false;
	}

	public static Direction getClosestDirection(BlockPos pos) {
		Vec3 playerCenter = MC.player.position();
		Vec3 eyePos = MC.player.getEyePosition();

		Direction best = null;
		double bestDistanceSqr = Double.MAX_VALUE;

		for (Direction dir : Direction.values()) {
			BlockPos neighbor = pos.relative(dir);
			if (!MC.level.getBlockState(neighbor).isAir())
				continue;

			Vec3 faceCenter = Vec3.atCenterOf(pos).add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);

			ClipContext context = new ClipContext(eyePos, faceCenter, ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, MC.player);
			BlockHitResult ray = MC.level.clip(context);
			if (ray.getType() == HitResult.Type.BLOCK && !pos.equals(ray.getBlockPos()))
				continue;

			double distanceSq = faceCenter.distanceToSqr(playerCenter);

			if (distanceSq < bestDistanceSqr) {
				bestDistanceSqr = distanceSq;
				best = dir;
			}
		}

		return best;
	}

	public static BlockPos findNearestBlockOf(Block block, float radius) {
		BlockPos playerPos = MC.player.blockPosition();
		Vec3 playerCenter = MC.player.position();
		double radiusSquared = radius * radius;
		int searchExtent = (int) Math.ceil(radius);

		BlockPos nearest = null;
		double nearestDistSq = Double.MAX_VALUE;

		for (int x = -searchExtent; x <= searchExtent; x++) {
			for (int y = -searchExtent; y <= searchExtent; y++) {
				for (int z = -searchExtent; z <= searchExtent; z++) {
					BlockPos pos = playerPos.offset(x, y, z);
					double distSq = Vec3.atCenterOf(pos).distanceToSqr(playerCenter);
					if (distSq > radiusSquared)
						continue;
					if (!MC.level.getBlockState(pos).is(block))
						continue;
					if (distSq < nearestDistSq) {
						nearestDistSq = distSq;
						nearest = pos;
					}
				}
			}
		}
		return nearest;
	}
}
