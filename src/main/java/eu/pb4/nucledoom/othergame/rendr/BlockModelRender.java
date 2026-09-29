package eu.pb4.nucledoom.othergame.rendr;

import com.mojang.math.Quadrant;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelElement;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelTransformation;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.util.*;

public class BlockModelRender {
    private final HashMap<String, Texture> textures = new HashMap<>();
    private final List<ModelElement> model = new ArrayList<>();
    private ModelTransformation transformation = new ModelTransformation(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO);

    public BlockModelRender() {
    }

    public void loadModel(Identifier modelId) {
        var jar = PolymerCommonUtils.getClientJarRoot();

        var modelStack = new ArrayList<ModelAsset>();
        var modelToLoad = modelId;
        while (true) {
            var path = jar.resolve(AssetPaths.model(modelToLoad) + ".json");

            if (!Files.exists(path)) {
                break;
            }

            try {
                var model = ModelAsset.fromJson(Files.readString(path));
                modelStack.add(model);
                if (model.parent().isPresent()) {
                    modelToLoad = model.parent().get();
                } else {
                    break;
                }
            } catch (Throwable e) {
                break;
            }
        }
        var unresolvedTextures = new HashMap<String, String>();
        List<ModelElement> list = null;
        var transform = new ModelTransformation(Vec3.ZERO, Vec3.ZERO, new Vec3(1, 1, 1));

        for (var model : modelStack.reversed()) {
            for (var x : model.textures().entrySet()) {
                unresolvedTextures.put(x.getKey(), x.getValue().getSerializedName());
            }

            if (model.elements().isPresent()) {
                list = model.elements().get();
            }

            if (model.display().containsKey(ItemDisplayContext.GUI)) {
                transform = model.display().get(ItemDisplayContext.GUI);
            }
        }

        this.transformation = transform;
        this.textures.clear();
        this.model.clear();

        for (var txt : unresolvedTextures.entrySet()) {
            var id = Identifier.tryParse(txt.getValue());
            if (id == null) {
                continue;
            }

            var path = jar.resolve(AssetPaths.texture(id) + ".png");

            if (!Files.exists(path)) {
                continue;
            }

            try {
                var image = Texture.fromImage(ImageIO.read(Files.newInputStream(path)));
                this.textures.put("#" + txt.getKey(), image);
                this.textures.put(txt.getKey(), image);
            } catch (Throwable e) {

            }
        }

        for (var txt : unresolvedTextures.entrySet()) {
            if (!txt.getValue().startsWith("#")) {
                continue;
            }
            this.textures.put("#" + txt.getKey(), this.textures.get(txt.getValue()));
            this.textures.put(txt.getKey(), this.textures.get(txt.getValue()));
        }


        if (list == null && modelToLoad.getNamespace().equals("minecraft") && modelToLoad.getPath().equals("builtin/generated")) {
            list = new ArrayList<>();

            for (var i = 0; i < 16; i++) {
                var id = "#layer" + i;
                var texture = textures.get(id);
                if (texture == null) continue;

                var scaleX = 16f / texture.width();
                var scaleY = 16f / texture.height();
                list.add(new ModelElement(new Vec3(0, 0, 8), new Vec3(16, 16, 9), Map.of(
                        Direction.SOUTH, new ModelElement.Face(FloatList.of(0, 0, 16, 16), id, Optional.empty(), Quadrant.R0, i),
                        Direction.NORTH, new ModelElement.Face(FloatList.of(16, 0, 0, 16), id, Optional.empty(), Quadrant.R0, i)
                )));

                for (int x = 0; x < texture.width(); x++) {
                    for (int y = 0; y < texture.height(); y++) {
                        var c = texture.get(x, y);
                        if (ARGB.alphaFloat(c) < 0.1) continue;
                        var u = x * scaleX + scaleX / 2;
                        var v = y * scaleY + scaleY / 2;

                        var face = new ModelElement.Face(FloatList.of(u, v, u, v), id, Optional.empty(), Quadrant.R0, i);
                        var map = new EnumMap<Direction, ModelElement.Face>(Direction.class);
                        for (var dir : Direction.values()) {
                            if (dir.getAxis() == Direction.Axis.Z) {
                                continue;
                            }

                            var u2 = (int) (u + dir.getStepX());
                            var v2 = (int) (v - dir.getStepY());

                            if (u2 >= 0 && v2 >= 0 && u2 < texture.width() && v2 < texture.height() && ARGB.alphaFloat(texture.get(u2, v2)) > 0.1) {
                                continue;
                            }

                            map.put(dir, face);
                        }

                        list.add(new ModelElement(new Vec3(x * scaleX, 16 - y * scaleY - scaleY, 8), new Vec3(x * scaleX + scaleX, 16 - y * scaleY, 9), map));
                    }
                }
            }
        } else if (list == null) {
            list = List.of();
        }

        this.model.addAll(list);
    }

    public void render(RenderOutput output, Matrix4f mat) {
        var transform = this.transformation;

        var view = new Matrix4fStack(4);
        view.mul(mat);

        view.translate(transform.translation().toVector3f());
        view.rotateXYZ(transform.rotation().toVector3f().mul(Mth.DEG_TO_RAD));
        view.scale(transform.scale().toVector3f());

        view.translate(-8, -8, -8);


        var vec = new Vector3f[]{
                new Vector3f(),
                new Vector3f(),
                new Vector3f(),
                new Vector3f()
        };

        var uv = new Vector2f[]{
                new Vector2f(0, 0),
                new Vector2f(0, 1),
                new Vector2f(1, 1),
                new Vector2f(1, 0),
        };

        for (var el : this.model) {
            view.pushMatrix();
            var from = el.from().toVector3f();
            var to = el.to().toVector3f();

            if (el.rotation().isPresent()) {
                var rot = el.rotation().get();
                var origin = rot.origin();
                view.translate(origin);
                if (rot.value() instanceof ModelElement.Rotation.SingleAxis(Direction.Axis axis, float angle)) {
                    view.rotate(angle * Mth.DEG_TO_RAD, axis.getPositive().getUnitVec3f());
                } else if (rot.value() instanceof ModelElement.Rotation.Euler(float x, float y, float z)) {
                    view.rotateXYZ(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD);
                }
                view.translate(origin.negate(new Vector3f()));
            }

            for (var face : el.faces().entrySet()) {
                var dir = face.getKey();
                var faceInfo = FaceInfo.fromFacing(dir);
                for (var i = 0; i < 4; i++) {
                    faceInfo.getVertexInfo(i).select(from, to, vec[i]);
                    view.transformProject(vec[i]);
                }

                var faceUv = face.getValue().uv();
                UVs uvs;

                if (!faceUv.isEmpty()) {
                    uvs = new UVs(faceUv.get(0), faceUv.get(1), faceUv.get(2), faceUv.get(3));
                } else {
                    uvs = UVs.defaultFaceUV(from, to, dir);
                }

                for (var i = 0; i < 4; i++) {
                    uv[i].set(uvs.getVertexU(i), uvs.getVertexV(i));
                }

                var brightness = switch (dir.getAxis()) {
                    case Z -> 0.5f;
                    case Y -> 1f;
                    case X -> 0.75f;
                };

                var color = ARGB.setBrightness(-1, brightness);
                output.drawQuad(0, vec, uv, this.textures.get(face.getValue().texture()), color);
            }
            view.popMatrix();
        }
    }
}
