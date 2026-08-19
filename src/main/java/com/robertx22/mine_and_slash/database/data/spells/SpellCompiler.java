package com.robertx22.mine_and_slash.database.data.spells;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.antlr.SpellLexer;
import com.robertx22.mine_and_slash.antlr.SpellParser;
import com.robertx22.mine_and_slash.antlr.SpellParser.*;
import com.robertx22.mine_and_slash.database.data.spells.components.BaseFieldNeeder;
import com.robertx22.mine_and_slash.database.data.spells.components.ComponentPart;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.conditions.EffectCondition;
import com.robertx22.mine_and_slash.database.data.spells.components.conditions.OrCondition;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.BaseTargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.registry.ExileDB;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class SpellCompiler {
    private static enum MapHolderType {
        ACTION(SpellAction.MAP),
        CONDITION(EffectCondition.MAP),
        SELECTOR(BaseTargetSelector.MAP);

        private final HashMap map;

        private MapHolderType(HashMap map) {
            this.map = map;
        }
    }

    private static abstract class CompilerError extends Exception {
        public int startLine;
        public int startChar;
        public int endLine;
        public int endChar;

        public CompilerError(ParserRuleContext context, String details) {
            super(details);
            startLine = context.start.getLine();
            startChar = context.start.getCharPositionInLine();
            endLine = context.stop.getLine();
            endChar = context.stop.getCharPositionInLine();
        }
        @Override
        public String toString() {
            return String.format("%d,%d: %s: %s", startLine, startChar, errorType(), getMessage());
        }
        public abstract String errorType();
    }

    private static class PropertyNameError extends CompilerError {
        Object accessObject;
        public PropertyNameError(ParserRuleContext context, Object accessObject, String details) {
            super(context, details);
            this.accessObject = accessObject;
        }
        @Override public String errorType() { return String.format("No such property in %s", accessObject.getClass().getSimpleName()); }
    }

    private static class PropertyTypeError extends CompilerError {
        Object accessObject;
        public PropertyTypeError(ParserRuleContext context, Object accessObject, String details) {
            super(context, details);
            this.accessObject = accessObject;
        }
        @Override public String errorType() { return String.format("Accessed property as wrong type in %s", accessObject.getClass().getSimpleName()); }
    }

    private static class PropertyEnumError extends CompilerError {
        Object accessObject;
        public PropertyEnumError(ParserRuleContext context, Object accessObject, String details) {
            super(context, details);
            this.accessObject = accessObject;
        }
        @Override public String errorType() { return String.format("Passed invalid value for enum in %s", accessObject.getClass().getSimpleName()); }
    }

    private static class MapHolderNameError extends CompilerError {
        String type;
        public MapHolderNameError(ParserRuleContext context, String name, MapHolderType type) {
            super(context, name);
            this.type = type.toString().toLowerCase();
        }
        @Override public String errorType() { return String.format("No such %s", type); }
    }

    private static class SyntaxError extends CompilerError {
        public SyntaxError(ParserRuleContext context, String details) { super(context, details); }
        @Override public String errorType() { return "Syntax error"; }
    }

    private static class ValueWrapper {
        Object value;
    };

    private static class ConditionHolder {
        private static enum Type {
            IF,
            EN_PRED
        };
        MapHolder map;
        Type type;
    };

    public static void compileSpells(ExileRegistryType type, ResourceManager manager, String name, Gson gson, Map<ResourceLocation, JsonElement> output) {
        var converter = new FileToIdConverter(name, ".spell");

        ExileLog.get().log("Parsing spell sources");

        converter.listMatchingResources(manager).forEach((location, resource) -> {
            var fileId = converter.fileToId(location);

            ExileLog.get().log("Parsing spell source {}", location.getPath());

            BufferedReader reader;
            CodePointCharStream charStream;

            try {
                reader = resource.openAsReader();
                charStream = CharStreams.fromReader(reader);
            } catch (IOException exception) {
                ExileLog.get().warn("IOException while reading " + location.toString() + "\n" + exception.toString());
                JsonExileRegistry.addToErroredJsons(type, fileId);
                return;
            }

            var compiler = new SpellCompiler(charStream, fileId);
            JsonElement spellJson;

            try {
                spellJson = compiler.compile();
            } catch (CompilerError error) {
                ExileLog.get().warn("{}:{}", location.toString(), error.toString());
                JsonExileRegistry.addToErroredJsons(type, fileId);
                return;
            } finally {
                try {
                    reader.close();
                } catch (IOException exception) {
                    ExileLog.get().warn("{}: IOException while closing\n{}", location.toString(), exception.toString());
                    JsonExileRegistry.addToErroredJsons(type, fileId);
                    return;
                }
            }

            output.put(fileId, spellJson);
        });
    }

    private interface IfNotNullFunction<T extends ParseTree> {
        void accept(T node) throws CompilerError;
    }

    private interface ElseFunction {
        void run() throws CompilerError;
    }

    private static abstract class ElseIfNotNull {
        public abstract <T extends ParseTree> ElseIfNotNull elseIfNotNull(T node, IfNotNullFunction<T> consumer) throws CompilerError;
        public abstract <T extends ParseTree> void elseDo(ElseFunction runnable) throws CompilerError;
    }

    private static class TrueElseIfNotNull extends ElseIfNotNull {
        @Override
        public <T extends ParseTree> ElseIfNotNull elseIfNotNull(T node, IfNotNullFunction<T> consumer) throws CompilerError {
            return new TrueElseIfNotNull();
        }
        @Override
        public <T extends ParseTree> void elseDo(ElseFunction runnable) throws CompilerError {
        }
    }

    private static class FalseElseIfNotNull extends ElseIfNotNull {
        @Override
        public <T extends ParseTree> ElseIfNotNull elseIfNotNull(T node, IfNotNullFunction<T> consumer) throws CompilerError {
            return ifNotNull(node, consumer);
        }
        @Override
        public <T extends ParseTree> void elseDo(ElseFunction runnable) throws CompilerError {
            runnable.run();
        }
    }

    private static <T extends ParseTree> ElseIfNotNull ifNotNull(T node, IfNotNullFunction<T> consumer) throws CompilerError {
        if (node != null) {
            consumer.accept(node);
            return new TrueElseIfNotNull();
        } else {
            return new FalseElseIfNotNull();
        }
    }

    private SpellLexer lexer;
    private TokenStream tokenStream;
    private SpellParser parser;
    private ResourceLocation fileId;
    private Spell spell;

    // each nested json subobject we're accessing
    private Deque<Object> objectAccessStack = new ArrayDeque<>();

    // nested conditional blocks
    private Deque<List<ConditionHolder>> conditionStack = new ArrayDeque<>();

    // nested select blocks
    private Deque<List<MapHolder>> selectorStack = new ArrayDeque<>();

    private SpellCompiler(CodePointCharStream charStream, ResourceLocation fileId) {
        this.lexer = new SpellLexer(charStream);
        this.tokenStream = new CommonTokenStream(lexer);
        this.parser = new SpellParser(tokenStream);
        this.fileId = fileId;
    }

    private JsonElement compile() throws CompilerError {
        spell = new Spell();
        spell.identifier = fileId.getPath();
        objectAccessStack.push(spell);

        var root = parser.spell();

        for (var statement : root.statement()) {
            ifNotNull(statement.propertyStatement(), propertyStatement -> {
                handlePropertyStatement(propertyStatement);
            }).elseIfNotNull(statement.attachedStatement(), attachedStatement -> {
                handleAttachedStatement(attachedStatement);
            });
        }

        return spell.toJson();
    }

    private void handlePropertyBlock(PropertyBlockContext node) throws CompilerError {
        for (var statement : node.propertyStatement()) {
            handlePropertyStatement(statement);
        }
    }

    private void handlePropertyStatement(PropertyStatementContext node) throws CompilerError {
        ifNotNull(node.subobject(), subobject -> {
            handleSubobject(subobject);
        }).elseIfNotNull(node.assignment(), assignment -> {
            handleAssignment(assignment);
        });
    }

    private void handleAssignment(AssignmentContext node) throws CompilerError {
        var propertyName = node.propertyName().getText();

        ifNotNull(node.propertyValue().literalArrayValue(), literalArrayValue -> {
            // handle arrays of primitives
            var elements = new ArrayList();
            for (var literal : literalArrayValue.literal()) {
                elements.add(handleLiteral(literal));
            }
            writeTopObjectFieldCollection(node, propertyName, elements);
        }).elseIfNotNull(node.propertyValue().objectArrayValue(), objectArrayValue -> {
            // handle arrays of objects
            writeTopObjectFieldObjectCollection(node, propertyName, elementConstructor -> {
                var elements = new ArrayList();

                for (var elementBlock : objectArrayValue.arrayElement()) {
                    // descend into each array element
                    var element = elementConstructor.newInstance();
                    objectAccessStack.push(element);
                    for (var statement : elementBlock.propertyStatement()) {
                        handlePropertyStatement(statement);
                    }
                    objectAccessStack.pop();
                    elements.add(element);
                }

                return elements;
            });
        }).elseDo(() -> {
            // handle primitive values
            var propertyValue = handleLiteral(node.propertyValue().literal());
            writeTopObjectField(node, propertyName, propertyValue);
        });
    }

    private Object handleLiteral(LiteralContext node) throws CompilerError {
        var value = new ValueWrapper();

        ifNotNull(node.String(), asString -> {
            var text = asString.getText(); // includes quotes
            value.value = text.substring(1, text.length() - 1);
        }).elseIfNotNull(node.Double(), asDouble -> {
            value.value = Double.parseDouble(asDouble.getText());
        }).elseIfNotNull(node.Int(), asInt -> {
            value.value = Integer.parseInt(asInt.getText());
        }).elseIfNotNull(node.bool(), asBool -> {
            value.value = asBool.True() != null;
        });

        return value.value;
    }

    private void handleSubobject(SubobjectContext node) throws CompilerError {
        var name = node.subobjectName().getText();
        objectAccessStack.push(readTopObjectField(node, name));
        handlePropertyBlock(node.propertyBlock());
        objectAccessStack.pop();
    }

    private void handleAttachedStatement(AttachedStatementContext node) throws CompilerError {
        ifNotNull(node.eventHandler(), (eventHandler) -> {
            handleEventHandler(eventHandler);
        }).elseDo(() -> {
            handleEntity(node.entity());
        });
    }

    private void handleEventHandler(EventHandlerContext node) throws CompilerError {
        var parts = handleScriptBlock(node.scriptBlock());

        if (node.eventName().OnCast() != null) {
            spell.attached.on_cast.addAll(parts);
        } else if (node.eventName().OnTick() != null) {
            //spell.attached.on_tick.addAll(parts);
        } else if (node.eventName().OnCastEnd() != null) {
            //spell.attached.on_cast_end.addAll(parts);
        }
    }

    private void handleEntity(EntityContext node) throws CompilerError {
        var parts = handleScriptBlock(node.scriptBlock());
        var name = node.entityName().toString();
        spell.attached.entity_components.put(name, parts);
    }

    // returns list of spell parts
    private List<ComponentPart> handleScriptBlock(ScriptBlockContext node) throws CompilerError {
        var parts = new ArrayList<ComponentPart>();

        for (var statement : node.scriptStatement()) {
            ifNotNull(statement.ifBlock(), ifBlock -> {
                parts.addAll(handleIfBlock(ifBlock));
            }).elseIfNotNull(statement.selectBlock(), selectBlock -> {
                parts.addAll(handleSelectBlock(selectBlock));
            }).elseIfNotNull(statement.actions(), actions -> {
                parts.add(handleActions(actions));
            });
        }

        return parts;
    }

    // action list / spell part
    private ComponentPart handleActions(ActionsContext node) throws CompilerError {
        var part = new ComponentPart();

        for (var action : node.mapHolder()) {
            part.acts.add(handleAction(action));
        }

        for (var conditions : conditionStack) {
            for (var condition : conditions) {
                switch (condition.type) {
                case IF:
                    part.ifs.add(condition.map);
                    break;
                case EN_PRED:
                    part.en_preds.add(condition.map);
                    break;
                }
            }
        }

        selectorStack.forEach(conditions -> part.targets.addAll(conditions));

        ifNotNull(node.perEntityHit(), perEntityHit -> {
            // don't propagate conditions and selectors to per_entity_hit block
            var oldConditionStack = conditionStack;
            var oldSelectorStack = selectorStack;
            conditionStack = new ArrayDeque();
            selectorStack = new ArrayDeque();
            part.per_entity_hit = handleScriptBlock(perEntityHit.scriptBlock());
            conditionStack = oldConditionStack;
            selectorStack = oldSelectorStack;
        });

        return part;
    }

    // returns list of spell parts
    private List<ComponentPart> handleIfBlock(IfBlockContext node) throws CompilerError {
        var conditions = handleConditionExpr(node.conditionExpr());
        conditionStack.push(conditions);

        var parts = handleScriptBlock(node.scriptBlock());

        ifNotNull(node.elseBlock(), elseBlock -> {
            invertConditions(conditions);
            parts.addAll(handleScriptBlock(elseBlock.scriptBlock()));
        });

        conditionStack.pop();
        return parts;
    }

    // returns list of conditions
    private List<ConditionHolder> handleConditionExpr(ConditionExprContext node) throws CompilerError {
        return handleConditionAnd(node.conditionAnd());
    }

    // returns list of conditions
    private List<ConditionHolder> handleConditionAnd(ConditionAndContext node) throws CompilerError {
        var conditions = new ArrayList<ConditionHolder>();
        for (var child : node.conditionNot()) {
            conditions.addAll(handleConditionNot(child));
        }
        return conditions;
    }

    // returns list of conditions
    private List<ConditionHolder> handleConditionNot(ConditionNotContext node) throws CompilerError {
        var conditions = handleConditionParen(node.conditionParen());
        if (node.Not() != null) {
            invertConditions(conditions);
        }
        return conditions;
    }

    private void invertConditions(List<ConditionHolder> conditions) {
        if (conditions.size() == 1) {
            if (conditions.get(0).map.type == EffectCondition.OR.GUID()) {
                // to invert an OR, we invert the conditions and turn it into an AND
                var conditions =
            }
        }

        for (var condition : conditions) {
            if (condition.map.has(MapField.IS_FALSE) && condition.map.get(MapField.IS_FALSE)) {
                condition.map.remove(MapField.IS_FALSE);
            } else {
                condition.map.put(MapField.IS_FALSE, true);
            }
        }
    }

    // returns list of conditions
    private List<ConditionHolder> handleConditionParen(ConditionParenContext node) throws CompilerError {
        if (node.condition() != null) {
            return List.of(handleCondition(node.condition()));
        } else {
            return handleConditionExpr(node.conditionExpr());
        }
    }

    // returns list of spell parts
    private List<ComponentPart> handleSelectBlock(SelectBlockContext node) throws CompilerError {
        var selectors = new ArrayList<MapHolder>();

        for (var selector : node.selectorList().selector()) {
            selectors.add(handleSelector(selector));
        }

        selectorStack.push(selectors);
        var parts = handleScriptBlock(node.scriptBlock());
        selectorStack.pop();

        return parts;
    }

    // returns single action
    private MapHolder handleAction(MapHolderContext node) throws CompilerError {
        return handleMapHolder(node, MapHolderType.ACTION);
    }

    // returns single condition
    private ConditionHolder handleCondition(ConditionContext node) throws CompilerError {
        var condition = new ConditionHolder();
        condition.map = handleMapHolder(node.mapHolder(), MapHolderType.CONDITION);
        condition.type = node.enPredPrefix() != null ? ConditionHolder.Type.EN_PRED : ConditionHolder.Type.IF;
        return condition;
    }

    // returns single selector
    private MapHolder handleSelector(SelectorContext node) throws CompilerError {
        return handleMapHolder(node.mapHolder(), MapHolderType.SELECTOR);
    }

    private MapHolder handleMapHolder(MapHolderContext node, MapHolderType type) throws CompilerError {
        var map = new MapHolder();
        map.type = node.mapHolderType().getText();

        if (!type.map.containsKey(map.type)) {
            throw new MapHolderNameError(node, map.type, type);
        }

        var args = node.mapHolderArguments();
        var typeObject = (BaseFieldNeeder) type.map.get(map.type);
        var requiredArgCount = typeObject.requiredPieces.size();
        var positionalArgCount = args != null ? args.argumentPositional().size() : 0;

        if (positionalArgCount > requiredArgCount) {
            throw new SyntaxError(
                node, String.format("Expected at most %d positional arguments to '%s', got %d",
                    requiredArgCount, map.type, positionalArgCount));
        }

        if (args != null) {
            for (var i = 0; i < positionalArgCount; i++) {
                var positionalArg = args.argumentPositional(i);
                var field = typeObject.requiredPieces.get(i);
                var value = handleLiteral(positionalArg.literal());
                addToMapHolder(map, field, value);
            }

            for (var keywordArg : args.argumentKeyValue()) {
                var fieldName = keywordArg.argumentKey().getText();

                if (!MapField.MAP.containsKey(fieldName)) {
                    throw new SyntaxError(node, String.format("Unknown argument name '%s'", fieldName));
                }

                var field = MapField.MAP.get(fieldName);
                var value = handleLiteral(keywordArg.argumentValue().literal());
                addToMapHolder(map, field, value);
            }
        }

        for (var requiredField : typeObject.requiredPieces) {
            if (!map.has(requiredField)) {
                throw new SyntaxError(node, String.format("Missing required field '%s'", requiredField.GUID()));
            }
        }

        return map;
    }

    private void addToMapHolder(MapHolder map, MapField field, Object value) {
        if (field == MapField.VALUE_CALCULATION && value instanceof String valueCalcName) {
            map.put(field, ExileDB.ValueCalculations().get(valueCalcName));
        } else {
            map.put(field, value);
        }
    }

    private Object getTopObject() {
        return objectAccessStack.getFirst();
    }

    private Field getObjectField(ParserRuleContext node, Object object, String name) throws CompilerError {
        Field field;
        try {
            field = object.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new PropertyNameError(node, object, e.getMessage());
        }

        field.setAccessible(true);
        return field;
    }

    private Object readTopObjectField(ParserRuleContext context, String name) throws CompilerError {
        var topObject = getTopObject();
        var field = getObjectField(context, topObject, name);
        try {
            return field.get(topObject);
        } catch (IllegalAccessException e) {
            throw new PropertyTypeError(context, topObject, e.getMessage());
        }
    }

    private void writeTopObjectField(ParserRuleContext context, String name, Object value) throws CompilerError {
        var topObject = getTopObject();
        var field = getObjectField(context, topObject, name);
        var fieldType = field.getType();

        if (fieldType.equals(float.class) && value instanceof Double doubleValue) {
            // allow assigning double literals to float fields
            value = (float) (double) doubleValue;
        } else if (fieldType.isEnum() && value instanceof String enumValue) {
            // allow assigning string literals to enum fields
            try {
                value = Enum.valueOf((Class) fieldType, enumValue);
            } catch (Exception e) {
                throw new PropertyEnumError(context, topObject, e.getMessage());
            }
        }

        try {
            field.set(topObject, value);
        } catch (Exception e) {
            throw new PropertyTypeError(context, topObject, e.getMessage());
        }
    }

    private void writeTopObjectFieldCollection(ParserRuleContext context, String name, Collection value) throws CompilerError {
        var topObject = getTopObject();
        var field = getObjectField(context, topObject, name);
        try {
            var collection = (Collection) field.get(topObject);
            collection.clear();
            collection.addAll(value);
        } catch (Exception e) {
            throw new PropertyTypeError(context, topObject, e.getMessage());
        }
    }

    private interface CollectionProducerFunction {
        Collection produce(Constructor elementConstructor) throws Exception;
    }

    // for writing to lists of objects, allows default constructing elements
    private void writeTopObjectFieldObjectCollection(ParserRuleContext context, String name, CollectionProducerFunction producer) throws CompilerError {
        var topObject = getTopObject();
        var field = getObjectField(context, topObject, name);
        try {
            var fieldGenericType = (ParameterizedType) field.getGenericType();
            var elementType = (Class) fieldGenericType.getActualTypeArguments()[0];
            var constructor = elementType.getDeclaredConstructor();
            constructor.setAccessible(true);
            var collection = (Collection) field.get(topObject);
            collection.clear();
            collection.addAll(producer.produce(elementType.getConstructor()));
        } catch (CompilerError e) {
            throw e;
        } catch (Exception e) {
            throw new PropertyTypeError(context, topObject, e.getMessage());
        }
    }
}
