package mathjx.extensiblefood.block.condition;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPredicateArgumentType.BlockPredicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public final class BlockPredicateCondition extends BlockStayCondition {

	private final Vec3i offset;
	private final BlockPredicate predicate;

	public BlockPredicateCondition(final Vec3i offset, final BlockPredicate predicate) {
		this.offset = offset;
		this.predicate = predicate;
	}

	@Override
	public boolean test(final BlockState state, final World world, final BlockPos pos) {
		try {
			return predicate.create(Registry.BLOCK).test(new CachedBlockPosition(world, pos.add(offset), false));
		} catch (final CommandSyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}

}
