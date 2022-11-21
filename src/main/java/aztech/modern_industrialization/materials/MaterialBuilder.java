/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.materials;

import aztech.modern_industrialization.materials.part.BuildablePart;
import aztech.modern_industrialization.materials.part.MaterialPart;
import aztech.modern_industrialization.materials.part.Part;
import aztech.modern_industrialization.materials.property.MaterialProperty;
import aztech.modern_industrialization.materials.recipe.builder.MaterialRecipeBuilder;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.data.recipes.FinishedRecipe;

public final class MaterialBuilder {

    private final Map<String, MaterialPart> partsMap = new TreeMap<>();
    private final Map<MaterialProperty<?>, Object> properties = new IdentityHashMap<>();
    private final PartContext partContext = new PartContext();
    private final String englishName;
    private final String materialName;

    private final Queue<RecipeAction> recipesActions = new LinkedList<>();

    public MaterialBuilder(String englishName, String materialName) {
        this.englishName = englishName;
        this.materialName = materialName;

        for (var prop : MaterialProperty.PROPERTIES) {
            properties.put(prop, prop.defaultValue);
        }
    }

    public String getMaterialName() {
        return materialName;
    }

    public <T> MaterialBuilder set(MaterialProperty<T> prop, T value) {
        this.properties.put(prop, value);
        return this;
    }

    public MaterialBuilder addParts(BuildablePart... parts) {
        for (BuildablePart part : parts) {
            addPart(part.build(partContext));
        }
        return this;
    }

    public MaterialBuilder addParts(List<BuildablePart> parts) {
        for (BuildablePart part : parts) {
            addPart(part.build(partContext));
        }
        return this;
    }

    public MaterialBuilder addParts(MaterialPart... parts) {
        for (MaterialPart part : parts) {
            addPart(part);
        }
        return this;
    }

    public MaterialBuilder removeParts(Part... parts) {
        for (Part part : parts) {
            removePart(part);
        }
        return this;
    }

    private void addPart(MaterialPart part) {
        if (partsMap.put(part.getPart().key, part) != null) {
            throw new IllegalStateException("Part " + part.getItemId() + " is already registered for this material!");
        }
    }

    private void removePart(Part part) {
        partsMap.remove(part.key);
    }

    public MaterialBuilder overridePart(MaterialPart part) {
        if (partsMap.put(part.getPart().key, part) == null) {
            throw new IllegalStateException("Part " + part.getItemId() + " was not already registered for this material!");
        }
        return this;
    }

    @SafeVarargs
    public final MaterialBuilder addRecipes(Consumer<RecipeContext>... consumers) {
        recipesActions.add(new RecipeAction() {
            @Override
            void apply(RecipeContext recipeContext) {
                for (Consumer<RecipeContext> consumer : consumers) {
                    consumer.accept(recipeContext);
                }
            }
        });
        return this;
    }

    public MaterialBuilder cancelRecipes(String... recipeIds) {
        recipesActions.add(new RecipeAction() {
            @Override
            void apply(RecipeContext context) {
                for (String recipeId : recipeIds) {
                    context.removeRecipe(recipeId);
                }
            }
        });
        return this;
    }

    public final Material build(RegisteringEvent... events) {
        var context = new PartContext();
        for (MaterialPart part : partsMap.values()) {
            part.register(context);
        }

        for (RegisteringEvent event : events) {
            event.onRegister(context);
        }

        return new Material(materialName, properties, Collections.unmodifiableMap(partsMap), this::buildRecipes);
    }

    public void buildRecipes(Consumer<FinishedRecipe> consumer) {
        Map<String, MaterialRecipeBuilder> recipesMap = new HashMap<>();
        RecipeContext recipeContext = new RecipeContext(recipesMap);
        for (RecipeAction action : recipesActions) {
            action.apply(recipeContext);
        }
        for (MaterialRecipeBuilder builder : recipesMap.values()) {
            // noinspection deprecation
            builder.save(consumer);
        }
    }

    public class PartContext {

        public String getMaterialName() {
            return materialName;
        }

        public String getEnglishName() {
            return englishName;
        }

        public MaterialPart getMaterialPart(Part part) {
            return partsMap.get(part.key);
        }

        public <T> T get(MaterialProperty<T> prop) {
            return (T) properties.get(prop);
        }
    }

    public class RecipeContext {
        private final Map<String, MaterialRecipeBuilder> recipesMap;

        public RecipeContext(Map<String, MaterialRecipeBuilder> recipesMap) {
            this.recipesMap = recipesMap;
        }

        public void addRecipe(MaterialRecipeBuilder builder) {
            if (recipesMap.containsKey(builder.getRecipeId())) {
                if (recipesMap.get(builder.getRecipeId()).isCanceled()) {
                    recipesMap.remove(builder.getRecipeId());
                } else {
                    throw new IllegalStateException(
                            "Duplicate registration of recipe " + builder.getRecipeId() + " for Material : " + getMaterialName());
                }
            }
            recipesMap.put(builder.getRecipeId(), builder);
        }

        public void removeRecipe(String recipeId) {
            if (recipesMap.remove(recipeId) == null) {
                throw new IllegalArgumentException("Recipe does not exist and cannot be cancelled: " + recipeId + " for Material : " + materialName);
            }
        }

        public MaterialPart getPart(Part part) {
            return partsMap.get(part.key);
        }

        public String getMaterialName() {
            return materialName;
        }

        public <T> T get(MaterialProperty<T> prop) {
            return (T) properties.get(prop);
        }
    }

    public abstract class RecipeAction {

        abstract void apply(RecipeContext context);

    }

    @FunctionalInterface
    public interface RegisteringEvent {

        void onRegister(PartContext context);
    }
}
