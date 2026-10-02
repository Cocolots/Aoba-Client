package net.aoba.mixin.interfaces;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface IMouseHandler {
	@Invoker("onMove")
	void executeOnCursorPos(long window, double x, double y, double xrel, double yrel);

    @Invoker("onScroll")
    void executeOnMouseScroll(long window, double horizontal, double vertical);

    @Invoker("onButton")
    void executeOnMouseButton(long window, MouseButtonInfo buttonInfo, int action);
}

