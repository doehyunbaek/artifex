/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.ArrayTypeSignature
import org.opalj.br.ClassTypeSignature
import org.opalj.br.ConstantDouble
import org.opalj.br.ConstantFloat
import org.opalj.br.ConstantInteger
import org.opalj.br.ConstantLong
import org.opalj.br.ConstantString
import org.opalj.br.Deprecated
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PCs
import org.opalj.br.ProperTypeArgument
import org.opalj.br.ReferenceType
import org.opalj.br.RuntimeInvisibleAnnotationTable
import org.opalj.br.RuntimeVisibleAnnotationTable
import org.opalj.br.SimpleClassTypeSignature
import org.opalj.br.SourceFile
import org.opalj.br.Synthetic
import org.opalj.br.TypeVariableSignature
import org.opalj.br.Wildcard
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.MutableType
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.EUBPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.properties.TACAI

import scala.collection.immutable.SortedSet

class AdHocFieldImmutabilityAnalysis private[analyses] (val project: SomeProject)
  extends FPCFAnalysis {

    case class State(
                      field:                          Field,
                      var fieldImmutabilityDependees: Set[EOptionP[Entity, Property]]             = Set.empty,
                      var genericTypeParameters:      SortedSet[String]                           = SortedSet.empty,
                      var upperBound:                 FieldImmutability                           = TransitivelyImmutableField,
                      var lowerBound:                 FieldImmutability                           = MutableField,
                      var tacDependees:               Map[Method, (EOptionP[Method, TACAI], PCs)] = Map.empty,
                      var innerTypes:                 Set[ReferenceType]                          = Set.empty,
                      var concreteClassTypeIsKnown:   Boolean                                     = true
                    ) {
        def hasDependees: Boolean = fieldImmutabilityDependees.nonEmpty || tacDependees.nonEmpty
        def getDependees: Set[EOptionP[Entity, Property]] =
            fieldImmutabilityDependees ++ tacDependees.valuesIterator.map(_._1)
    }

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def doDetermineFieldImmutability(entity: Entity): ProperPropertyComputationResult = entity match {
        case field: Field => determineFieldImmutability(field)
        case _ =>
            throw new IllegalArgumentException(s"${entity.getClass.getName} is not an org.opalj.br.Field")
    }

    private[analyses] def determineFieldImmutability(
                                                      field: Field
                                                    ): ProperPropertyComputationResult = {

        implicit val state: State = State(field)

        def determineTypeImmutability(implicit state: State): Unit = {
            if (state.field.fieldType == ObjectType.Object) {
                // in case of a field with type object: field immutability stays NonTransitivelyImmutable
                if (state.genericTypeParameters.isEmpty)
                    state.upperBound = NonTransitivelyImmutableField
                //handle generics -> potentially unsound
            } else if (state.field.fieldType.isArrayType) {
                //Because the entries of an array can be reassigned we state it mutable
                state.upperBound = NonTransitivelyImmutableField
            } else {
                checkTypeImmutability(propertyStore(state.field.fieldType.asReferenceType, TypeImmutability.key))
            }
        }

        def checkTypeImmutability(result: EOptionP[ReferenceType, TypeImmutability])(implicit state: State): Unit = result match {
            case LBP(TransitivelyImmutableType) => // transitively immutable type is set as default

            case ep @ EUBPS(t, DependentlyImmutableType(_), _) =>
                // if the inner type of a generic field is dependently immutable
                if (state.innerTypes.contains(t.asReferenceType) ||
                  // or there are no generic information it is over-approximated to non-transitively immutable
                  (state.genericTypeParameters.isEmpty && state.innerTypes.isEmpty))
                    state.upperBound = NonTransitivelyImmutableField
                else if (ep.isRefinable)
                //if a field as a dep imm type that is refinable it could get worse and therefor dependencies are stored
                    state.fieldImmutabilityDependees += ep

            //Will be recognized in determineDependentImmutability
            //Here the upper bound is not changed to recognize concretized transitively immutable fields
            case UBP(MutableType | NonTransitivelyImmutableType) =>
                state.upperBound = NonTransitivelyImmutableField
            case ep => state.fieldImmutabilityDependees += ep
        }

        def determineClassImmutability(referenceType: ReferenceType)(implicit state: State): Unit = {
            if (referenceType.isArrayType)
                state.upperBound = NonTransitivelyImmutableField
            else
                checkClassImmutability(propertyStore(referenceType, ClassImmutability.key))
        }

        def checkClassImmutability(result: EOptionP[ReferenceType, ClassImmutability])(implicit state: State): Unit =
            result match {

                case LBP(TransitivelyImmutableClass) => //transitively immutable is default

                case ep @ EUBP(t, DependentlyImmutableClass(_)) =>
                    // if the inner type of a generic field is dependently immutable
                    if (state.innerTypes.contains(t.asReferenceType) ||
                      // or there are no generic information it is over-approximated to non-transitively immutable
                      (state.genericTypeParameters.isEmpty && state.innerTypes.isEmpty))
                        state.upperBound = NonTransitivelyImmutableField
                    else if (ep.isRefinable)
                    //if a field as a dep imm type that is refinable it could get worse and therefor dependencies are stored
                        state.fieldImmutabilityDependees += ep

                case EUBP(c, MutableClass) if (field.fieldType == ObjectType.Object && c == ObjectType.Object) => {

                    state.field.attributes.foreach {
                        case TypeVariableSignature(_) =>
                            state.upperBound =
                                DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
                        case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) =>
                            typeArguments.foreach {
                                case ProperTypeArgument(_, TypeVariableSignature(_)) =>
                                    state.upperBound =
                                        DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
                                case _ =>
                            }
                        case _ =>
                    }
                    if (!state.upperBound.isInstanceOf[DependentlyImmutableField])
                        state.upperBound = NonTransitivelyImmutableField
                }
                case UBP(MutableClass | NonTransitivelyImmutableClass) =>
                    state.upperBound = NonTransitivelyImmutableField

                case ep => state.fieldImmutabilityDependees += ep
            }

        def determineDependentImmutability()(implicit state: State): Unit = {

            def collectGenericIdentifierAndSetDependentImmutability(t: String): Unit = {
                state.genericTypeParameters += t
                setDependentImmutability()
            }

            def setDependentImmutability(): Unit = {
                state.upperBound = DependentlyImmutableField(state.genericTypeParameters).meet(state.upperBound)
            }

            state.field.attributes.foreach {

                case RuntimeInvisibleAnnotationTable(_) | SourceFile(_) => // no generic parameter

                case TypeVariableSignature(t) =>
                    collectGenericIdentifierAndSetDependentImmutability(t)

                case ClassTypeSignature(_, SimpleClassTypeSignature(_, typeArguments), _) =>
                    typeArguments.foreach {

                        case ProperTypeArgument(_, TypeVariableSignature(identifier)) =>
                            collectGenericIdentifierAndSetDependentImmutability(identifier)

                        case ProperTypeArgument(
                        _,
                        ClassTypeSignature(
                        outerPackageIdentifier,
                        SimpleClassTypeSignature(innerPackageIdentifier, _),
                        _
                        )
                        ) =>
                            val objectPath = outerPackageIdentifier match {
                                case Some(prepackageIdentifier) => prepackageIdentifier + innerPackageIdentifier
                                case _                          => innerPackageIdentifier
                            }
                            val objectType = ObjectType(objectPath)
                            state.innerTypes += objectType
                            checkTypeImmutability(propertyStore(objectType, TypeImmutability.key))
                        case ProperTypeArgument(_, ArrayTypeSignature(_)) =>
                            state.upperBound = NonTransitivelyImmutableField
                        case Wildcard =>
                        case _        => state.upperBound = NonTransitivelyImmutableField
                    }
                case ConstantInteger(_) | ConstantFloat(_) | ConstantString(_) | ConstantLong(_) | ConstantDouble(_) |
                     Synthetic | Deprecated |
                     RuntimeVisibleAnnotationTable(_) =>
                case ArrayTypeSignature(_) => state.upperBound = NonTransitivelyImmutableField
                case _                     => state.upperBound = NonTransitivelyImmutableField
            }
        }

        /**
         * Returns the TACode for a method if available, registering dependencies as necessary.
         */
        def getTACAI(
                      method: Method,
                      pcs:    PCs,
                      isRead: Boolean
                    )(implicit state: State): Option[TACode[TACMethodParameter, V]] = {
            propertyStore(method, TACAI.key) match {
                case finalEP: FinalEP[Method, TACAI] => finalEP.ub.tac
                case epk =>
                    val writes =
                        if (state.tacDependees.contains(method))
                            state.tacDependees(method)._2
                        else
                            IntTrieSet.empty
                    state.tacDependees += method -> ((epk, writes ++ pcs))
                    None
            }
        }

        /**
         * Determines whether the the field write information can lead to a concretization of the immutability.
         */
        def examineFieldWritesForConcretizationOfImmutability()(implicit state: State): Unit = {
            val writes = fieldAccessInformation.writeAccesses(state.field)
            writes.foreach { write =>
                val (method, pcs) = write
                val taCodeOption = getTACAI(method, pcs, isRead = false)
                if (taCodeOption.isDefined)
                    searchForConcreteObjectInFieldWritesWithKnownTAC(pcs, taCodeOption.get)
            }
        }

        /**
         * Determine whether concrete objects or more precise class type can be recognized in the field writes.
         */
        def searchForConcreteObjectInFieldWritesWithKnownTAC(
                                                              pcs:    PCs,
                                                              taCode: TACode[TACMethodParameter, V]
                                                            )(implicit state: State): Unit = {
            /**
             * Analyzes the put fields for concrete object or more precise class type information and adjusts the
             * immutability information.
             */
            def analyzePutForConcreteObjectsOrMorePreciseClassType(putStmt: Stmt[V]): Unit = {
                val (putDefinitionSites, putValue) = {
                    if (putStmt.isPutField) {
                        val putField = putStmt.asPutField
                        (putField.value.asVar.definedBy, putField.value.asVar)
                    } else if (putStmt.isPutStatic) {
                        val putStatic = putStmt.asPutStatic
                        (putStatic.value.asVar.definedBy, putStatic.value.asVar)
                    } else
                        throw new Exception(s"$putStmt is not a  putStmt")
                }
                if (putValue.value.isArrayValue.isYesOrUnknown) {
                    state.upperBound = NonTransitivelyImmutableField
                } else if (putValue.value.isArrayValue.isNoOrUnknown && putValue.value.isReferenceValue) {
                    putDefinitionSites.foreach { putDefinitionSite =>
                        if (putDefinitionSite < 0) {
                            state.concreteClassTypeIsKnown = false
                        } else {
                            val definitionSiteAssignment = taCode.stmts(putDefinitionSite).asAssignment
                            if (definitionSiteAssignment.expr.isNew) {
                                val newStmt = definitionSiteAssignment.expr.asNew
                                if (field.fieldType.isObjectType) {
                                    determineClassImmutability(newStmt.tpe.mostPreciseObjectType)
                                } else
                                    state.concreteClassTypeIsKnown = false
                            } else
                                state.concreteClassTypeIsKnown = false
                        }
                    }
                }
            }

            pcs.foreach { pc =>
                val index = taCode.pcToIndex(pc)
                if (index >= 0) {
                    val stmt = taCode.stmts(index)
                    analyzePutForConcreteObjectsOrMorePreciseClassType(stmt)
                }
            }
        }

        def checkFieldAssignability(result: EOptionP[Field, FieldAssignability])(implicit state: State): Unit = result match {

            case LBP(NonAssignable | EffectivelyNonAssignable | LazilyInitialized) =>
                state.lowerBound = NonTransitivelyImmutableField

            case UBP(Assignable | UnsafelyLazilyInitialized) =>
                state.upperBound = MutableField

            case ep => state.fieldImmutabilityDependees += ep
        }

        /**
         * Checks whether the type was a concretization of a generic type and adjusts the immutability information if
         * necessary.
         */
        def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
            if (eps.asEPS.pk != TACAI.key)
                state.fieldImmutabilityDependees =
                    state.fieldImmutabilityDependees.filter(ep => (ep.e != eps.e) || (ep.pk != eps.pk))
            eps.ub.key match {

                case ClassImmutability.key =>
                    checkClassImmutability(eps.asInstanceOf[EOptionP[ReferenceType, ClassImmutability]])

                case FieldAssignability.key =>
                    checkFieldAssignability(eps.asInstanceOf[EOptionP[Field, FieldAssignability]])

                case TypeImmutability.key =>
                    checkTypeImmutability(eps.asInstanceOf[EOptionP[ObjectType, TypeImmutability]])

                case TACAI.key =>
                    val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                    val method = newEP.e
                    val pcs = state.tacDependees(method)._2
                    state.tacDependees -= method
                    if (eps.isFinal) {
                        val tac = eps.asInstanceOf[FinalEP[Method, TACAI]].p.tac.get
                        searchForConcreteObjectInFieldWritesWithKnownTAC(pcs, tac)(state)
                        if (!state.concreteClassTypeIsKnown)
                            determineTypeImmutability
                    } else {
                        state.tacDependees += method -> ((newEP, pcs))
                    }
            }
            createResult()
        }

        /**
         * If there are no dependencies left, this method can be called to create the result.
         */
        def createResult()(implicit state: State): ProperPropertyComputationResult = {
            if (state.lowerBound == state.upperBound || !state.hasDependees)
                Result(field, state.upperBound)
            else
                InterimResult(field, state.lowerBound, state.upperBound, state.getDependees, c)
        }

        checkFieldAssignability(propertyStore(field, FieldAssignability.key))
        if (state.upperBound == MutableField)
            return Result(field, MutableField);

        /**
         * Determines whether the field is dependently immutable
         */
        determineDependentImmutability()

        /**
         * Determines whether the immutability information can be precised due to more precise class or type information
         */
        examineFieldWritesForConcretizationOfImmutability()

        /**
         * In case of we know the concrete class type assigned to the field we could use the immutability of this.
         */
        if (!state.concreteClassTypeIsKnown && state.tacDependees.isEmpty)
            determineTypeImmutability

        createResult()
    }
}

trait AdHocFieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(FieldImmutability)
    )

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        FieldAccessInformationKey,
        ClosedPackagesKey,
        TypeExtensibilityKey,
        DefinitionSitesKey
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldImmutability)
}

/**
 *
 * Executor for the eager field immutability analysis.
 *
 */
object EagerAdHocFieldImmutabilityAnalysis
  extends AdHocFieldImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new AdHocFieldImmutabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldImmutability)
        analysis
    }
}

/**
 *
 * Executor for the lazy field immutability analysis.
 *
 */
object LazyAdHocCHAFieldImmutabilityAnalysis
  extends AdHocFieldImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    final override def register(
                                 p:      SomeProject,
                                 ps:     PropertyStore,
                                 unused: Null
                               ): FPCFAnalysis = {
        import org.opalj.br.fpcf.properties.immutability.FieldImmutability
        val analysis = new AdHocFieldImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldImmutability.key,
            analysis.determineFieldImmutability
        )
        analysis
    }
}
