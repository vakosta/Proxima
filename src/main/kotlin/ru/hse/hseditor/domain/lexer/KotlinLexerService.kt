package ru.hse.hseditor.domain.lexer

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import ru.hse.hseditor.antlr.KotlinLexer
import ru.hse.hseditor.antlr.KotlinParser
import ru.hse.hseditor.antlr.KotlinParserListener

class KotlinLexerService(
    private val text: String,
) : LexerService {

    override val allTokens: List<Token>
        get() = getTokens()

    private fun getTokens(): List<Token> {
        val lexer = KotlinLexer(CharStreams.fromString(text))
        val parser = KotlinParser(CommonTokenStream(lexer))
        parser.addParseListener(parserListener)
        parser.statements()
        lexer.reset()
        return lexer.allTokens
    }

    companion object {
        private val parserListener = object : KotlinParserListener {
            override fun visitTerminal(node: TerminalNode?) {}

            override fun visitErrorNode(node: ErrorNode?) {}

            override fun enterEveryRule(ctx: ParserRuleContext?) {}

            override fun exitEveryRule(ctx: ParserRuleContext?) {}

            override fun enterKotlinFile(ctx: KotlinParser.KotlinFileContext?) {}

            override fun exitKotlinFile(ctx: KotlinParser.KotlinFileContext?) {}

            override fun enterScript(ctx: KotlinParser.ScriptContext?) {}

            override fun exitScript(ctx: KotlinParser.ScriptContext?) {}

            override fun enterPreamble(ctx: KotlinParser.PreambleContext?) {}

            override fun exitPreamble(ctx: KotlinParser.PreambleContext?) {}

            override fun enterFileAnnotations(ctx: KotlinParser.FileAnnotationsContext?) {}

            override fun exitFileAnnotations(ctx: KotlinParser.FileAnnotationsContext?) {}

            override fun enterFileAnnotation(ctx: KotlinParser.FileAnnotationContext?) {}

            override fun exitFileAnnotation(ctx: KotlinParser.FileAnnotationContext?) {}

            override fun enterPackageHeader(ctx: KotlinParser.PackageHeaderContext?) {}

            override fun exitPackageHeader(ctx: KotlinParser.PackageHeaderContext?) {}

            override fun enterImportList(ctx: KotlinParser.ImportListContext?) {}

            override fun exitImportList(ctx: KotlinParser.ImportListContext?) {}

            override fun enterImportHeader(ctx: KotlinParser.ImportHeaderContext?) {}

            override fun exitImportHeader(ctx: KotlinParser.ImportHeaderContext?) {}

            override fun enterImportAlias(ctx: KotlinParser.ImportAliasContext?) {}

            override fun exitImportAlias(ctx: KotlinParser.ImportAliasContext?) {}

            override fun enterTopLevelObject(ctx: KotlinParser.TopLevelObjectContext?) {}

            override fun exitTopLevelObject(ctx: KotlinParser.TopLevelObjectContext?) {}

            override fun enterClassDeclaration(ctx: KotlinParser.ClassDeclarationContext?) {}

            override fun exitClassDeclaration(ctx: KotlinParser.ClassDeclarationContext?) {}

            override fun enterPrimaryConstructor(ctx: KotlinParser.PrimaryConstructorContext?) {}

            override fun exitPrimaryConstructor(ctx: KotlinParser.PrimaryConstructorContext?) {}

            override fun enterClassParameters(ctx: KotlinParser.ClassParametersContext?) {}

            override fun exitClassParameters(ctx: KotlinParser.ClassParametersContext?) {}

            override fun enterClassParameter(ctx: KotlinParser.ClassParameterContext?) {}

            override fun exitClassParameter(ctx: KotlinParser.ClassParameterContext?) {}

            override fun enterDelegationSpecifiers(ctx: KotlinParser.DelegationSpecifiersContext?) {}

            override fun exitDelegationSpecifiers(ctx: KotlinParser.DelegationSpecifiersContext?) {}

            override fun enterDelegationSpecifier(ctx: KotlinParser.DelegationSpecifierContext?) {}

            override fun exitDelegationSpecifier(ctx: KotlinParser.DelegationSpecifierContext?) {}

            override fun enterConstructorInvocation(ctx: KotlinParser.ConstructorInvocationContext?) {}

            override fun exitConstructorInvocation(ctx: KotlinParser.ConstructorInvocationContext?) {}

            override fun enterExplicitDelegation(ctx: KotlinParser.ExplicitDelegationContext?) {}

            override fun exitExplicitDelegation(ctx: KotlinParser.ExplicitDelegationContext?) {}

            override fun enterClassBody(ctx: KotlinParser.ClassBodyContext?) {}

            override fun exitClassBody(ctx: KotlinParser.ClassBodyContext?) {}

            override fun enterClassMemberDeclaration(ctx: KotlinParser.ClassMemberDeclarationContext?) {}

            override fun exitClassMemberDeclaration(ctx: KotlinParser.ClassMemberDeclarationContext?) {}

            override fun enterAnonymousInitializer(ctx: KotlinParser.AnonymousInitializerContext?) {}

            override fun exitAnonymousInitializer(ctx: KotlinParser.AnonymousInitializerContext?) {}

            override fun enterSecondaryConstructor(ctx: KotlinParser.SecondaryConstructorContext?) {}

            override fun exitSecondaryConstructor(ctx: KotlinParser.SecondaryConstructorContext?) {}

            override fun enterConstructorDelegationCall(ctx: KotlinParser.ConstructorDelegationCallContext?) {}

            override fun exitConstructorDelegationCall(ctx: KotlinParser.ConstructorDelegationCallContext?) {}

            override fun enterEnumClassBody(ctx: KotlinParser.EnumClassBodyContext?) {}

            override fun exitEnumClassBody(ctx: KotlinParser.EnumClassBodyContext?) {}

            override fun enterEnumEntries(ctx: KotlinParser.EnumEntriesContext?) {}

            override fun exitEnumEntries(ctx: KotlinParser.EnumEntriesContext?) {}

            override fun enterEnumEntry(ctx: KotlinParser.EnumEntryContext?) {}

            override fun exitEnumEntry(ctx: KotlinParser.EnumEntryContext?) {}

            override fun enterFunctionDeclaration(ctx: KotlinParser.FunctionDeclarationContext?) {}

            override fun exitFunctionDeclaration(ctx: KotlinParser.FunctionDeclarationContext?) {}

            override fun enterFunctionValueParameters(ctx: KotlinParser.FunctionValueParametersContext?) {}

            override fun exitFunctionValueParameters(ctx: KotlinParser.FunctionValueParametersContext?) {}

            override fun enterFunctionValueParameter(ctx: KotlinParser.FunctionValueParameterContext?) {}

            override fun exitFunctionValueParameter(ctx: KotlinParser.FunctionValueParameterContext?) {}

            override fun enterParameter(ctx: KotlinParser.ParameterContext?) {}

            override fun exitParameter(ctx: KotlinParser.ParameterContext?) {}

            override fun enterFunctionBody(ctx: KotlinParser.FunctionBodyContext?) {}

            override fun exitFunctionBody(ctx: KotlinParser.FunctionBodyContext?) {}

            override fun enterObjectDeclaration(ctx: KotlinParser.ObjectDeclarationContext?) {}

            override fun exitObjectDeclaration(ctx: KotlinParser.ObjectDeclarationContext?) {}

            override fun enterCompanionObject(ctx: KotlinParser.CompanionObjectContext?) {}

            override fun exitCompanionObject(ctx: KotlinParser.CompanionObjectContext?) {}

            override fun enterPropertyDeclaration(ctx: KotlinParser.PropertyDeclarationContext?) {}

            override fun exitPropertyDeclaration(ctx: KotlinParser.PropertyDeclarationContext?) {}

            override fun enterMultiVariableDeclaration(ctx: KotlinParser.MultiVariableDeclarationContext?) {}

            override fun exitMultiVariableDeclaration(ctx: KotlinParser.MultiVariableDeclarationContext?) {}

            override fun enterVariableDeclaration(ctx: KotlinParser.VariableDeclarationContext?) {}

            override fun exitVariableDeclaration(ctx: KotlinParser.VariableDeclarationContext?) {}

            override fun enterGetter(ctx: KotlinParser.GetterContext?) {}

            override fun exitGetter(ctx: KotlinParser.GetterContext?) {}

            override fun enterSetter(ctx: KotlinParser.SetterContext?) {}

            override fun exitSetter(ctx: KotlinParser.SetterContext?) {}

            override fun enterTypeAlias(ctx: KotlinParser.TypeAliasContext?) {}

            override fun exitTypeAlias(ctx: KotlinParser.TypeAliasContext?) {}

            override fun enterTypeParameters(ctx: KotlinParser.TypeParametersContext?) {}

            override fun exitTypeParameters(ctx: KotlinParser.TypeParametersContext?) {}

            override fun enterTypeParameter(ctx: KotlinParser.TypeParameterContext?) {}

            override fun exitTypeParameter(ctx: KotlinParser.TypeParameterContext?) {}

            override fun enterType(ctx: KotlinParser.TypeContext?) {}

            override fun exitType(ctx: KotlinParser.TypeContext?) {}

            override fun enterTypeModifierList(ctx: KotlinParser.TypeModifierListContext?) {}

            override fun exitTypeModifierList(ctx: KotlinParser.TypeModifierListContext?) {}

            override fun enterParenthesizedType(ctx: KotlinParser.ParenthesizedTypeContext?) {}

            override fun exitParenthesizedType(ctx: KotlinParser.ParenthesizedTypeContext?) {}

            override fun enterNullableType(ctx: KotlinParser.NullableTypeContext?) {}

            override fun exitNullableType(ctx: KotlinParser.NullableTypeContext?) {}

            override fun enterTypeReference(ctx: KotlinParser.TypeReferenceContext?) {}

            override fun exitTypeReference(ctx: KotlinParser.TypeReferenceContext?) {}

            override fun enterFunctionType(ctx: KotlinParser.FunctionTypeContext?) {}

            override fun exitFunctionType(ctx: KotlinParser.FunctionTypeContext?) {}

            override fun enterFunctionTypeReceiver(ctx: KotlinParser.FunctionTypeReceiverContext?) {}

            override fun exitFunctionTypeReceiver(ctx: KotlinParser.FunctionTypeReceiverContext?) {}

            override fun enterUserType(ctx: KotlinParser.UserTypeContext?) {}

            override fun exitUserType(ctx: KotlinParser.UserTypeContext?) {}

            override fun enterSimpleUserType(ctx: KotlinParser.SimpleUserTypeContext?) {}

            override fun exitSimpleUserType(ctx: KotlinParser.SimpleUserTypeContext?) {}

            override fun enterFunctionTypeParameters(ctx: KotlinParser.FunctionTypeParametersContext?) {}

            override fun exitFunctionTypeParameters(ctx: KotlinParser.FunctionTypeParametersContext?) {}

            override fun enterTypeConstraints(ctx: KotlinParser.TypeConstraintsContext?) {}

            override fun exitTypeConstraints(ctx: KotlinParser.TypeConstraintsContext?) {}

            override fun enterTypeConstraint(ctx: KotlinParser.TypeConstraintContext?) {}

            override fun exitTypeConstraint(ctx: KotlinParser.TypeConstraintContext?) {}

            override fun enterBlock(ctx: KotlinParser.BlockContext?) {}

            override fun exitBlock(ctx: KotlinParser.BlockContext?) {}

            override fun enterStatements(ctx: KotlinParser.StatementsContext?) {}

            override fun exitStatements(ctx: KotlinParser.StatementsContext?) {}

            override fun enterStatement(ctx: KotlinParser.StatementContext?) {}

            override fun exitStatement(ctx: KotlinParser.StatementContext?) {}

            override fun enterBlockLevelExpression(ctx: KotlinParser.BlockLevelExpressionContext?) {}

            override fun exitBlockLevelExpression(ctx: KotlinParser.BlockLevelExpressionContext?) {}

            override fun enterDeclaration(ctx: KotlinParser.DeclarationContext?) {}

            override fun exitDeclaration(ctx: KotlinParser.DeclarationContext?) {}

            override fun enterExpression(ctx: KotlinParser.ExpressionContext?) {}

            override fun exitExpression(ctx: KotlinParser.ExpressionContext?) {}

            override fun enterDisjunction(ctx: KotlinParser.DisjunctionContext?) {}

            override fun exitDisjunction(ctx: KotlinParser.DisjunctionContext?) {}

            override fun enterConjunction(ctx: KotlinParser.ConjunctionContext?) {}

            override fun exitConjunction(ctx: KotlinParser.ConjunctionContext?) {}

            override fun enterEqualityComparison(ctx: KotlinParser.EqualityComparisonContext?) {}

            override fun exitEqualityComparison(ctx: KotlinParser.EqualityComparisonContext?) {}

            override fun enterComparison(ctx: KotlinParser.ComparisonContext?) {}

            override fun exitComparison(ctx: KotlinParser.ComparisonContext?) {}

            override fun enterNamedInfix(ctx: KotlinParser.NamedInfixContext?) {}

            override fun exitNamedInfix(ctx: KotlinParser.NamedInfixContext?) {}

            override fun enterElvisExpression(ctx: KotlinParser.ElvisExpressionContext?) {}

            override fun exitElvisExpression(ctx: KotlinParser.ElvisExpressionContext?) {}

            override fun enterInfixFunctionCall(ctx: KotlinParser.InfixFunctionCallContext?) {}

            override fun exitInfixFunctionCall(ctx: KotlinParser.InfixFunctionCallContext?) {}

            override fun enterRangeExpression(ctx: KotlinParser.RangeExpressionContext?) {}

            override fun exitRangeExpression(ctx: KotlinParser.RangeExpressionContext?) {}

            override fun enterAdditiveExpression(ctx: KotlinParser.AdditiveExpressionContext?) {}

            override fun exitAdditiveExpression(ctx: KotlinParser.AdditiveExpressionContext?) {}

            override fun enterMultiplicativeExpression(ctx: KotlinParser.MultiplicativeExpressionContext?) {}

            override fun exitMultiplicativeExpression(ctx: KotlinParser.MultiplicativeExpressionContext?) {}

            override fun enterTypeRHS(ctx: KotlinParser.TypeRHSContext?) {}

            override fun exitTypeRHS(ctx: KotlinParser.TypeRHSContext?) {}

            override fun enterPrefixUnaryExpression(ctx: KotlinParser.PrefixUnaryExpressionContext?) {}

            override fun exitPrefixUnaryExpression(ctx: KotlinParser.PrefixUnaryExpressionContext?) {}

            override fun enterPostfixUnaryExpression(ctx: KotlinParser.PostfixUnaryExpressionContext?) {}

            override fun exitPostfixUnaryExpression(ctx: KotlinParser.PostfixUnaryExpressionContext?) {}

            override fun enterAtomicExpression(ctx: KotlinParser.AtomicExpressionContext?) {}

            override fun exitAtomicExpression(ctx: KotlinParser.AtomicExpressionContext?) {}

            override fun enterParenthesizedExpression(ctx: KotlinParser.ParenthesizedExpressionContext?) {}

            override fun exitParenthesizedExpression(ctx: KotlinParser.ParenthesizedExpressionContext?) {}

            override fun enterCallSuffix(ctx: KotlinParser.CallSuffixContext?) {}

            override fun exitCallSuffix(ctx: KotlinParser.CallSuffixContext?) {}

            override fun enterAnnotatedLambda(ctx: KotlinParser.AnnotatedLambdaContext?) {}

            override fun exitAnnotatedLambda(ctx: KotlinParser.AnnotatedLambdaContext?) {}

            override fun enterArrayAccess(ctx: KotlinParser.ArrayAccessContext?) {}

            override fun exitArrayAccess(ctx: KotlinParser.ArrayAccessContext?) {}

            override fun enterValueArguments(ctx: KotlinParser.ValueArgumentsContext?) {}

            override fun exitValueArguments(ctx: KotlinParser.ValueArgumentsContext?) {}

            override fun enterTypeArguments(ctx: KotlinParser.TypeArgumentsContext?) {}

            override fun exitTypeArguments(ctx: KotlinParser.TypeArgumentsContext?) {}

            override fun enterTypeProjection(ctx: KotlinParser.TypeProjectionContext?) {}

            override fun exitTypeProjection(ctx: KotlinParser.TypeProjectionContext?) {}

            override fun enterTypeProjectionModifierList(ctx: KotlinParser.TypeProjectionModifierListContext?) {}

            override fun exitTypeProjectionModifierList(ctx: KotlinParser.TypeProjectionModifierListContext?) {}

            override fun enterValueArgument(ctx: KotlinParser.ValueArgumentContext?) {}

            override fun exitValueArgument(ctx: KotlinParser.ValueArgumentContext?) {}

            override fun enterLiteralConstant(ctx: KotlinParser.LiteralConstantContext?) {}

            override fun exitLiteralConstant(ctx: KotlinParser.LiteralConstantContext?) {}

            override fun enterStringLiteral(ctx: KotlinParser.StringLiteralContext?) {}

            override fun exitStringLiteral(ctx: KotlinParser.StringLiteralContext?) {}

            override fun enterLineStringLiteral(ctx: KotlinParser.LineStringLiteralContext?) {}

            override fun exitLineStringLiteral(ctx: KotlinParser.LineStringLiteralContext?) {}

            override fun enterMultiLineStringLiteral(ctx: KotlinParser.MultiLineStringLiteralContext?) {}

            override fun exitMultiLineStringLiteral(ctx: KotlinParser.MultiLineStringLiteralContext?) {}

            override fun enterLineStringContent(ctx: KotlinParser.LineStringContentContext?) {}

            override fun exitLineStringContent(ctx: KotlinParser.LineStringContentContext?) {}

            override fun enterLineStringExpression(ctx: KotlinParser.LineStringExpressionContext?) {}

            override fun exitLineStringExpression(ctx: KotlinParser.LineStringExpressionContext?) {}

            override fun enterMultiLineStringContent(ctx: KotlinParser.MultiLineStringContentContext?) {}

            override fun exitMultiLineStringContent(ctx: KotlinParser.MultiLineStringContentContext?) {}

            override fun enterMultiLineStringExpression(ctx: KotlinParser.MultiLineStringExpressionContext?) {}

            override fun exitMultiLineStringExpression(ctx: KotlinParser.MultiLineStringExpressionContext?) {}

            override fun enterFunctionLiteral(ctx: KotlinParser.FunctionLiteralContext?) {}

            override fun exitFunctionLiteral(ctx: KotlinParser.FunctionLiteralContext?) {}

            override fun enterLambdaParameters(ctx: KotlinParser.LambdaParametersContext?) {}

            override fun exitLambdaParameters(ctx: KotlinParser.LambdaParametersContext?) {}

            override fun enterLambdaParameter(ctx: KotlinParser.LambdaParameterContext?) {}

            override fun exitLambdaParameter(ctx: KotlinParser.LambdaParameterContext?) {}

            override fun enterObjectLiteral(ctx: KotlinParser.ObjectLiteralContext?) {}

            override fun exitObjectLiteral(ctx: KotlinParser.ObjectLiteralContext?) {}

            override fun enterCollectionLiteral(ctx: KotlinParser.CollectionLiteralContext?) {}

            override fun exitCollectionLiteral(ctx: KotlinParser.CollectionLiteralContext?) {}

            override fun enterThisExpression(ctx: KotlinParser.ThisExpressionContext?) {}

            override fun exitThisExpression(ctx: KotlinParser.ThisExpressionContext?) {}

            override fun enterSuperExpression(ctx: KotlinParser.SuperExpressionContext?) {}

            override fun exitSuperExpression(ctx: KotlinParser.SuperExpressionContext?) {}

            override fun enterConditionalExpression(ctx: KotlinParser.ConditionalExpressionContext?) {}

            override fun exitConditionalExpression(ctx: KotlinParser.ConditionalExpressionContext?) {}

            override fun enterIfExpression(ctx: KotlinParser.IfExpressionContext?) {}

            override fun exitIfExpression(ctx: KotlinParser.IfExpressionContext?) {}

            override fun enterControlStructureBody(ctx: KotlinParser.ControlStructureBodyContext?) {}

            override fun exitControlStructureBody(ctx: KotlinParser.ControlStructureBodyContext?) {}

            override fun enterWhenExpression(ctx: KotlinParser.WhenExpressionContext?) {}

            override fun exitWhenExpression(ctx: KotlinParser.WhenExpressionContext?) {}

            override fun enterWhenEntry(ctx: KotlinParser.WhenEntryContext?) {}

            override fun exitWhenEntry(ctx: KotlinParser.WhenEntryContext?) {}

            override fun enterWhenCondition(ctx: KotlinParser.WhenConditionContext?) {}

            override fun exitWhenCondition(ctx: KotlinParser.WhenConditionContext?) {}

            override fun enterRangeTest(ctx: KotlinParser.RangeTestContext?) {}

            override fun exitRangeTest(ctx: KotlinParser.RangeTestContext?) {}

            override fun enterTypeTest(ctx: KotlinParser.TypeTestContext?) {}

            override fun exitTypeTest(ctx: KotlinParser.TypeTestContext?) {}

            override fun enterTryExpression(ctx: KotlinParser.TryExpressionContext?) {}

            override fun exitTryExpression(ctx: KotlinParser.TryExpressionContext?) {}

            override fun enterCatchBlock(ctx: KotlinParser.CatchBlockContext?) {}

            override fun exitCatchBlock(ctx: KotlinParser.CatchBlockContext?) {}

            override fun enterFinallyBlock(ctx: KotlinParser.FinallyBlockContext?) {}

            override fun exitFinallyBlock(ctx: KotlinParser.FinallyBlockContext?) {}

            override fun enterLoopExpression(ctx: KotlinParser.LoopExpressionContext?) {}

            override fun exitLoopExpression(ctx: KotlinParser.LoopExpressionContext?) {}

            override fun enterForExpression(ctx: KotlinParser.ForExpressionContext?) {}

            override fun exitForExpression(ctx: KotlinParser.ForExpressionContext?) {}

            override fun enterWhileExpression(ctx: KotlinParser.WhileExpressionContext?) {}

            override fun exitWhileExpression(ctx: KotlinParser.WhileExpressionContext?) {}

            override fun enterDoWhileExpression(ctx: KotlinParser.DoWhileExpressionContext?) {}

            override fun exitDoWhileExpression(ctx: KotlinParser.DoWhileExpressionContext?) {}

            override fun enterJumpExpression(ctx: KotlinParser.JumpExpressionContext?) {}

            override fun exitJumpExpression(ctx: KotlinParser.JumpExpressionContext?) {}

            override fun enterCallableReference(ctx: KotlinParser.CallableReferenceContext?) {}

            override fun exitCallableReference(ctx: KotlinParser.CallableReferenceContext?) {}

            override fun enterAssignmentOperator(ctx: KotlinParser.AssignmentOperatorContext?) {}

            override fun exitAssignmentOperator(ctx: KotlinParser.AssignmentOperatorContext?) {}

            override fun enterEqualityOperation(ctx: KotlinParser.EqualityOperationContext?) {}

            override fun exitEqualityOperation(ctx: KotlinParser.EqualityOperationContext?) {}

            override fun enterComparisonOperator(ctx: KotlinParser.ComparisonOperatorContext?) {}

            override fun exitComparisonOperator(ctx: KotlinParser.ComparisonOperatorContext?) {}

            override fun enterInOperator(ctx: KotlinParser.InOperatorContext?) {}

            override fun exitInOperator(ctx: KotlinParser.InOperatorContext?) {}

            override fun enterIsOperator(ctx: KotlinParser.IsOperatorContext?) {}

            override fun exitIsOperator(ctx: KotlinParser.IsOperatorContext?) {}

            override fun enterAdditiveOperator(ctx: KotlinParser.AdditiveOperatorContext?) {}

            override fun exitAdditiveOperator(ctx: KotlinParser.AdditiveOperatorContext?) {}

            override fun enterMultiplicativeOperation(ctx: KotlinParser.MultiplicativeOperationContext?) {}

            override fun exitMultiplicativeOperation(ctx: KotlinParser.MultiplicativeOperationContext?) {}

            override fun enterTypeOperation(ctx: KotlinParser.TypeOperationContext?) {}

            override fun exitTypeOperation(ctx: KotlinParser.TypeOperationContext?) {}

            override fun enterPrefixUnaryOperation(ctx: KotlinParser.PrefixUnaryOperationContext?) {}

            override fun exitPrefixUnaryOperation(ctx: KotlinParser.PrefixUnaryOperationContext?) {}

            override fun enterPostfixUnaryOperation(ctx: KotlinParser.PostfixUnaryOperationContext?) {}

            override fun exitPostfixUnaryOperation(ctx: KotlinParser.PostfixUnaryOperationContext?) {}

            override fun enterMemberAccessOperator(ctx: KotlinParser.MemberAccessOperatorContext?) {}

            override fun exitMemberAccessOperator(ctx: KotlinParser.MemberAccessOperatorContext?) {}

            override fun enterModifierList(ctx: KotlinParser.ModifierListContext?) {}

            override fun exitModifierList(ctx: KotlinParser.ModifierListContext?) {}

            override fun enterModifier(ctx: KotlinParser.ModifierContext?) {}

            override fun exitModifier(ctx: KotlinParser.ModifierContext?) {}

            override fun enterClassModifier(ctx: KotlinParser.ClassModifierContext?) {}

            override fun exitClassModifier(ctx: KotlinParser.ClassModifierContext?) {}

            override fun enterMemberModifier(ctx: KotlinParser.MemberModifierContext?) {}

            override fun exitMemberModifier(ctx: KotlinParser.MemberModifierContext?) {}

            override fun enterVisibilityModifier(ctx: KotlinParser.VisibilityModifierContext?) {}

            override fun exitVisibilityModifier(ctx: KotlinParser.VisibilityModifierContext?) {}

            override fun enterVarianceAnnotation(ctx: KotlinParser.VarianceAnnotationContext?) {}

            override fun exitVarianceAnnotation(ctx: KotlinParser.VarianceAnnotationContext?) {}

            override fun enterFunctionModifier(ctx: KotlinParser.FunctionModifierContext?) {}

            override fun exitFunctionModifier(ctx: KotlinParser.FunctionModifierContext?) {}

            override fun enterPropertyModifier(ctx: KotlinParser.PropertyModifierContext?) {}

            override fun exitPropertyModifier(ctx: KotlinParser.PropertyModifierContext?) {}

            override fun enterInheritanceModifier(ctx: KotlinParser.InheritanceModifierContext?) {}

            override fun exitInheritanceModifier(ctx: KotlinParser.InheritanceModifierContext?) {}

            override fun enterParameterModifier(ctx: KotlinParser.ParameterModifierContext?) {}

            override fun exitParameterModifier(ctx: KotlinParser.ParameterModifierContext?) {}

            override fun enterTypeParameterModifier(ctx: KotlinParser.TypeParameterModifierContext?) {}

            override fun exitTypeParameterModifier(ctx: KotlinParser.TypeParameterModifierContext?) {}

            override fun enterLabelDefinition(ctx: KotlinParser.LabelDefinitionContext?) {}

            override fun exitLabelDefinition(ctx: KotlinParser.LabelDefinitionContext?) {}

            override fun enterAnnotations(ctx: KotlinParser.AnnotationsContext?) {}

            override fun exitAnnotations(ctx: KotlinParser.AnnotationsContext?) {}

            override fun enterAnnotation(ctx: KotlinParser.AnnotationContext?) {}

            override fun exitAnnotation(ctx: KotlinParser.AnnotationContext?) {}

            override fun enterAnnotationList(ctx: KotlinParser.AnnotationListContext?) {}

            override fun exitAnnotationList(ctx: KotlinParser.AnnotationListContext?) {}

            override fun enterAnnotationUseSiteTarget(ctx: KotlinParser.AnnotationUseSiteTargetContext?) {}

            override fun exitAnnotationUseSiteTarget(ctx: KotlinParser.AnnotationUseSiteTargetContext?) {}

            override fun enterUnescapedAnnotation(ctx: KotlinParser.UnescapedAnnotationContext?) {}

            override fun exitUnescapedAnnotation(ctx: KotlinParser.UnescapedAnnotationContext?) {}

            override fun enterIdentifier(ctx: KotlinParser.IdentifierContext?) {}

            override fun exitIdentifier(ctx: KotlinParser.IdentifierContext?) {}

            override fun enterSimpleIdentifier(ctx: KotlinParser.SimpleIdentifierContext?) {}

            override fun exitSimpleIdentifier(ctx: KotlinParser.SimpleIdentifierContext?) {}

            override fun enterSemi(ctx: KotlinParser.SemiContext?) {}

            override fun exitSemi(ctx: KotlinParser.SemiContext?) {}

            override fun enterAnysemi(ctx: KotlinParser.AnysemiContext?) {}

            override fun exitAnysemi(ctx: KotlinParser.AnysemiContext?) {}
        }
    }
}
