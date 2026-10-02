package net.aoba.rendering.shaders;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;

import net.aoba.rendering.RenderApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class ShaderManager {
	private static final Logger LOGGER = LogUtils.getLogger();

	// Defines the built in shaders.
	private static final String BUILTIN_SHADER_PATH = "/assets/aoba/shaders/";
	private static final String[] BUILTIN_SHADER_IDS = { "rainbow", "blur", "blur_gradient", "gradient",
			"radial_gradient", "conical_gradient", "diamond_gradient", "image" };

	private final Map<String, Shader> shaders = new LinkedHashMap<>();

	public ShaderManager() {
		// Build default pipelines
		RenderPipeline solidPipeline2D = buildShaderPipeline2D("solid");
		RenderPipeline solidPipeline3D = buildShaderPipeline3D("solid");
		Shader solidShader = new Shader("solid", "Solid", "Solid color", RenderApi.current(), solidPipeline2D,
				solidPipeline3D, List.of(ShaderUniform.ofColor("Base Color", 1f, 1f, 1f, 1f)));
		shaders.put("solid", solidShader);

		// Load built-in shaders from resource JSON files
		for (String id : BUILTIN_SHADER_IDS) {
			String jsonPath = BUILTIN_SHADER_PATH + "aoba_" + id + ".json";
			try (InputStream is = ShaderManager.class.getResourceAsStream(jsonPath)) {
				if (is == null) {
					LOGGER.warn("[Aoba] Built-in shader JSON not found: {}", jsonPath);
					continue;
				}
				JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
				Shader shader = parseShader(id, json);
				if (shader != null) {
					shaders.put(id, shader);
					LOGGER.info("[Aoba] Loaded built-in shader: {}", shader.name());
				}
			} catch (Exception e) {
				LOGGER.error("[Aoba] Failed to load built-in shader: {}", id, e);
			}
		}

		// Load user shaders from /aoba/shaders/
		loadUserShaders();
	}

	/**
	 * Loads all of the user shaders from /aoba/shaders/
	 */
	private void loadUserShaders() {
		Path shadersDir = Minecraft.getInstance().gameDirectory.toPath().resolve("aoba/shaders");
		if (!Files.isDirectory(shadersDir))
			return;

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(shadersDir, "*.json")) {
			for (Path jsonFile : stream) {
				String fileName = jsonFile.getFileName().toString();
				String id = fileName.substring(0, fileName.length() - 5);

				if (shaders.containsKey(id)) {
					LOGGER.warn("[Aoba] User shader '{}' conflicts with existing shader, skipping", id);
					continue;
				}

				try {
					String jsonStr = Files.readString(jsonFile, StandardCharsets.UTF_8);
					JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
					Shader shader = parseShader(id, json);
					if (shader != null) {
						shaders.put(id, shader);
						LOGGER.info("[Aoba] Loaded user shader: {}", shader.name());
					}
				} catch (Exception e) {
					LOGGER.error("[Aoba] Failed to load user shader: {}", id, e);
				}
			}
		} catch (Exception e) {
			LOGGER.error("[Aoba] Failed to scan user shaders directory", e);
		}
	}

	/**
	 * Parses json representing the definition of a Shader and returns a built Shader object.
	 * @param id ID of the shader
	 * @param json Json representing the definition of the shader
	 * @return Resulting Shader parsed from the json.
	 */
	private Shader parseShader(String id, JsonObject json) {
		String name = json.has("name") ? json.get("name").getAsString() : id;
		String description = json.has("description") ? json.get("description").getAsString() : "";

		// Get the render backend that the shader is compatible with.
		RenderApi backend = RenderApi.OPENGL;
		if (json.has("backend")) {
			try {
				backend = RenderApi.valueOf(json.get("backend").getAsString().toUpperCase());
			} catch (IllegalArgumentException e) {
				LOGGER.warn("[Aoba] Unknown backend '{}' for shader '{}', defaulting to OPENGL",
						json.get("backend").getAsString(), id);
			}
		}

		// Gets the list of parameters of the shader.
		List<ShaderUniform> params = new ArrayList<>();
		if (json.has("params")) {
			JsonArray paramsArray = json.getAsJsonArray("params");
			for (JsonElement elem : paramsArray) {
				JsonObject p = elem.getAsJsonObject();
				ShaderUniform param = parseParam(p);
				if (param != null)
					params.add(param);
			}
		}

		// Creates a pipeline for both 3D and 2D.
		RenderPipeline pipeline2D = buildShaderPipeline2D(id);
		RenderPipeline pipeline3D = buildShaderPipeline3D(id);
		return new Shader(id, name, description, backend, pipeline2D, pipeline3D, params);
	}

	private ShaderUniform parseParam(JsonObject p) {
		String type = p.get("type").getAsString().toLowerCase();

		if (type.equals("time")) {
			return ShaderUniform.ofTime();
		} else if (type.equals("resolution")) {
			return ShaderUniform.ofResolution();
		}

		String name = p.get("name").getAsString();
		if (type.equals("float")) {
			float def = p.get("default").getAsFloat();
			float min = p.has("min") ? p.get("min").getAsFloat() : 0f;
			float max = p.has("max") ? p.get("max").getAsFloat() : 1f;
			float step = p.has("step") ? p.get("step").getAsFloat() : 0.1f;
			return ShaderUniform.ofFloat(name, def, min, max, step);
		} else if (type.equals("color")) {
			JsonArray def = p.getAsJsonArray("default");
			float r = def.get(0).getAsFloat();
			float g = def.get(1).getAsFloat();
			float b = def.get(2).getAsFloat();
			float a = def.size() > 3 ? def.get(3).getAsFloat() : 1.0f;
			return ShaderUniform.ofColor(name, r, g, b, a);
		} else if (type.equals("framebuffer")) {
			return ShaderUniform.ofFramebuffer(name);
		}

		LOGGER.warn("[Aoba] Unknown param type '{}' for param '{}'", type, name);
		return null;
	}

	private static Identifier aobaShader(String name) {
		return Identifier.fromNamespaceAndPath("aoba", "aoba_" + name);
	}

	// TODO: Can we somehow make these one?
	private static RenderPipeline buildShaderPipeline2D(String id) {
		return RenderPipelines.register(RenderPipeline.builder().withVertexShader(aobaShader(""))
				.withFragmentShader(aobaShader(id))
				.withBindGroupLayout(BindGroupLayout.builder()
						.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
						.withUniform("Projection", UniformType.UNIFORM_BUFFER)
						.withUniform("AobaShaderParams", UniformType.UNIFORM_BUFFER)
						.withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER).build())
				.withCull(false)
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
				.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
				.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
				.withLocation(Identifier.fromNamespaceAndPath("aoba", "pipeline/aoba_" + id)).build());
	}

	private static RenderPipeline buildShaderPipeline3D(String id) {
		return RenderPipelines.register(RenderPipeline.builder().withVertexShader(aobaShader(""))
				.withFragmentShader(aobaShader(id))
				.withBindGroupLayout(BindGroupLayout.builder()
						.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
						.withUniform("Projection", UniformType.UNIFORM_BUFFER)
						.withUniform("AobaShaderParams", UniformType.UNIFORM_BUFFER)
						.withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER).build())
				.withCull(false)
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
				.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
				.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
				.withLocation(Identifier.fromNamespaceAndPath("aoba", "pipeline/aoba_3d_" + id)).build());
	}

	/**
	 * Gets a shader with a given id.
	 * @param id Id of the shader to get.
	 * @return Shader if one is found.
	 */
	public Shader getShader(String id) {
		return shaders.get(id);
	}

	/**
	 * Returns all loaded shaders.
	 */
	public List<Shader> getAvailableShaders() {
		return Collections.unmodifiableList(new ArrayList<>(shaders.values()));
	}
}
