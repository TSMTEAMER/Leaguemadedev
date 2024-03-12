package kassuk.addon.blackout.utils.bed.port.basic;

import kassuk.addon.blackout.utils.bed.port.advance.RenderUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;


public class RenderInfo {
    public Render3DEvent event;
    public RenderUtils.RenderMode renderMode;
    public ShapeMode shapeMode;

    // Основной рендер
    public RenderInfo(Render3DEvent event, RenderUtils.RenderMode renderMode, ShapeMode shapeMode) {
        this.event = event;
        this.renderMode = renderMode;
        this.shapeMode = shapeMode;
    }

    // Рендер в случае если нет RenderMode
    public RenderInfo(Render3DEvent event, RenderUtils.RenderMode renderMode) {
        this.event = event;
        this.renderMode = renderMode;
    }
}
