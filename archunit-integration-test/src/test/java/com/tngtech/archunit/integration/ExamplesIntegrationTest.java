package com.tngtech.archunit.integration;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.example.AbstractController;
import com.tngtech.archunit.example.ClassViolatingCodingRules;
import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.ClassViolatingThirdPartyRules;
import com.tngtech.archunit.example.MyController;
import com.tngtech.archunit.example.MyService;
import com.tngtech.archunit.example.OtherClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.SecondBeanImplementingSomeBusinessInterface;
import com.tngtech.archunit.example.SomeBusinessInterface;
import com.tngtech.archunit.example.SomeCustomException;
import com.tngtech.archunit.example.SomeMediator;
import com.tngtech.archunit.example.anticorruption.WithIllegalReturnType;
import com.tngtech.archunit.example.anticorruption.WrappedResult;
import com.tngtech.archunit.example.anticorruption.internal.InternalType;
import com.tngtech.archunit.example.controller.SomeGuiController;
import com.tngtech.archunit.example.controller.SomeUtility;
import com.tngtech.archunit.example.controller.WronglyAnnotated;
import com.tngtech.archunit.example.controller.one.UseCaseOneThreeController;
import com.tngtech.archunit.example.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.controller.three.UseCaseThreeController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;
import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassOfMinimalCircleCallingSliceTwo;
import com.tngtech.archunit.example.cycle.complexcycles.slice1.SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.ClassOfMinimalCircleCallingSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.InstantiatedClassInSliceTwo;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.SliceTwoInheritingFromSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour;
import com.tngtech.archunit.example.cycle.complexcycles.slice3.ClassCallingConstructorInSliceFive;
import com.tngtech.archunit.example.cycle.complexcycles.slice3.InheritedClassInSliceThree;
import com.tngtech.archunit.example.cycle.complexcycles.slice4.ClassWithAccessedFieldCallingMethodInSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice5.InstantiatedClassInSliceFive;
import com.tngtech.archunit.example.cycle.constructorcycle.slice1.SliceOneCallingConstructorInSliceTwo;
import com.tngtech.archunit.example.cycle.constructorcycle.slice1.SomeClassWithCalledConstructor;
import com.tngtech.archunit.example.cycle.constructorcycle.slice2.SliceTwoCallingConstructorInSliceOne;
import com.tngtech.archunit.example.cycle.fieldaccesscycle.slice1.ClassInSliceOneWithAccessedField;
import com.tngtech.archunit.example.cycle.fieldaccesscycle.slice1.SliceOneAccessingFieldInSliceTwo;
import com.tngtech.archunit.example.cycle.fieldaccesscycle.slice2.SliceTwoAccessingFieldInSliceOne;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatCallSliceThree;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatInheritsFromSliceTwo;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatIsInheritedFromSliceTwo;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.InterfaceInSliceOne;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice2.ClassThatInheritsFromSliceOne;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice3.ClassThatImplementsInterfaceFromSliceOne;
import com.tngtech.archunit.example.cycle.simplecycle.slice1.SliceOneCallingMethodInSliceTwo;
import com.tngtech.archunit.example.cycle.simplecycle.slice1.SomeClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycle.simplecycle.slice2.SliceTwoCallingMethodOfSliceThree;
import com.tngtech.archunit.example.cycle.simplecycle.slice3.SliceThreeCallingMethodOfSliceOne;
import com.tngtech.archunit.example.cycle.simplescenario.administration.AdministrationService;
import com.tngtech.archunit.example.cycle.simplescenario.administration.Invoice;
import com.tngtech.archunit.example.cycle.simplescenario.importer.ImportService;
import com.tngtech.archunit.example.cycle.simplescenario.report.Report;
import com.tngtech.archunit.example.cycle.simplescenario.report.ReportService;
import com.tngtech.archunit.example.persistence.WrongSecurityCheck;
import com.tngtech.archunit.example.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.persistence.first.dao.EntityInWrongPackage;
import com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService;
import com.tngtech.archunit.example.service.ServiceInterface;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.example.service.impl.SomeInterfacePlacedInTheWrongPackage;
import com.tngtech.archunit.example.service.impl.WronglyNamedSvc;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.example.thirdparty.ThirdPartySubClassWithProblem;
import com.tngtech.archunit.example.web.AnnotatedController;
import com.tngtech.archunit.example.web.InheritedControllerImpl;
import com.tngtech.archunit.exampletest.SecurityTest;
import com.tngtech.archunit.testutils.CyclicErrorMatcher;
import com.tngtech.archunit.testutils.ExpectedTestFailures;
import com.tngtech.archunit.testutils.MessageAssertionChain;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.collect.Collections2.filter;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.example.OtherClassViolatingSessionBeanRules.init;
import static com.tngtech.archunit.example.SomeMediator.violateLayerRulesIndirectly;
import static com.tngtech.archunit.example.controller.one.UseCaseOneTwoController.doSomethingOne;
import static com.tngtech.archunit.example.controller.one.UseCaseOneTwoController.someString;
import static com.tngtech.archunit.example.controller.three.UseCaseThreeController.doSomethingThree;
import static com.tngtech.archunit.example.controller.two.UseCaseTwoController.doSomethingTwo;
import static com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService.violateLayerRules;
import static com.tngtech.archunit.example.service.ServiceViolatingLayerRules.illegalAccessToController;
import static com.tngtech.archunit.testutils.CyclicErrorMatcher.cycle;
import static com.tngtech.archunit.testutils.ExpectedAccess.accessFrom;
import static com.tngtech.archunit.testutils.ExpectedAccess.callFrom;
import static com.tngtech.archunit.testutils.ExpectedDependency.inheritanceFrom;
import static com.tngtech.archunit.testutils.ExpectedLocation.javaClass;
import static com.tngtech.archunit.testutils.ExpectedMethod.method;
import static com.tngtech.archunit.testutils.ExpectedNaming.simpleNameOf;
import static com.tngtech.archunit.testutils.ExpectedViolation.clazz;
import static com.tngtech.archunit.testutils.ExpectedViolation.javaPackageOf;
import static com.tngtech.archunit.testutils.SliceDependencyErrorMatcher.sliceDependency;
import static java.lang.System.lineSeparator;

class ExamplesIntegrationTest {

    @TestFactory
    Stream<DynamicTest> CodingRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.CodingRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.CodingRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.CodingRulesTest.class)

                .ofRule("no classes should access standard streams")
                .by(accessFrom(ClassViolatingCodingRules.class, "printToStandardStream")
                        .accessing().field(System.class, "out")
                        .inLine(12))
                .by(accessFrom(ClassViolatingCodingRules.class, "printToStandardStream")
                        .accessing().field(System.class, "err")
                        .inLine(13))
                .by(callFrom(ClassViolatingCodingRules.class, "printToStandardStream")
                        .toMethod(SomeCustomException.class, "printStackTrace")
                        .inLine(14))
                .by(accessFrom(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .accessing().field(System.class, "out")
                        .inLine(13))
                .times(2)

                .ofRule("no classes should throw generic exceptions")
                .by(accessFrom(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Throwable.class)
                        .inLine(22))
                .by(accessFrom(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Exception.class, String.class)
                        .inLine(24))
                .by(accessFrom(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(RuntimeException.class, String.class, Throwable.class)
                        .inLine(26))
                .by(accessFrom(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Exception.class, String.class)
                        .inLine(26))

                .ofRule("no classes should use java.util.logging")
                .by(accessFrom(ClassViolatingCodingRules.class, "<clinit>")
                        .setting().field(ClassViolatingCodingRules.class, "log")
                        .inLine(9))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> CyclicDependencyRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.CyclicDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.CyclicDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.CyclicDependencyRulesTest.class)

                .ofRule("slices matching '..(simplecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of simplecycle")
                        .by(accessFrom(SliceOneCallingMethodInSliceTwo.class, "callSliceTwo")
                                .toMethod(SliceTwoCallingMethodOfSliceThree.class, "doSomethingInSliceTwo")
                                .inLine(9))
                        .from("slice2 of simplecycle")
                        .by(accessFrom(SliceTwoCallingMethodOfSliceThree.class, "callSliceThree")
                                .toMethod(SliceThreeCallingMethodOfSliceOne.class, "doSomethingInSliceThree")
                                .inLine(9))
                        .from("slice3 of simplecycle")
                        .by(accessFrom(SliceThreeCallingMethodOfSliceOne.class, "callSliceOne")
                                .toMethod(SomeClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(9)))

                .ofRule("slices matching '..(constructorcycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of constructorcycle")
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwo.class, "callSliceTwo")
                                .toConstructor(SliceTwoCallingConstructorInSliceOne.class)
                                .inLine(7))
                        .from("slice2 of constructorcycle")
                        .by(accessFrom(SliceTwoCallingConstructorInSliceOne.class, "callSliceOne")
                                .toConstructor(SomeClassWithCalledConstructor.class)
                                .inLine(7)))

                .ofRule("slices matching '..(inheritancecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatInheritsFromSliceTwo.class)
                                .extending(ClassThatInheritsFromSliceOne.class))
                        .by(accessFrom(ClassThatInheritsFromSliceTwo.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatInheritsFromSliceOne.class)
                                .inLine(5))
                        .from("slice2 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatInheritsFromSliceOne.class)
                                .extending(ClassThatIsInheritedFromSliceTwo.class))
                        .by(accessFrom(ClassThatInheritsFromSliceOne.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatIsInheritedFromSliceTwo.class)
                                .inLine(5)))
                .by(cycle()
                        .from("slice1 of inheritancecycle")
                        .by(accessFrom(ClassThatCallSliceThree.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatImplementsInterfaceFromSliceOne.class)
                                .inLine(7))
                        .from("slice3 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatImplementsInterfaceFromSliceOne.class)
                                .implementing(InterfaceInSliceOne.class)))

                .ofRule("slices matching '..(fieldaccesscycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of fieldaccesscycle")
                        .by(accessFrom(SliceOneAccessingFieldInSliceTwo.class, "accessSliceTwo")
                                .setting().field(SliceTwoAccessingFieldInSliceOne.class, "accessedField")
                                .inLine(9))
                        .from("slice2 of fieldaccesscycle")
                        .by(accessFrom(SliceTwoAccessingFieldInSliceOne.class, "accessSliceOne")
                                .setting().field(ClassInSliceOneWithAccessedField.class, "accessedField")
                                .inLine(10)))

                .ofRule("slices matching '..simplescenario.(*)..' should be free of cycles")
                .by(cycle().from("administration")
                        .by(accessFrom(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(ReportService.class, "getReport", String.class)
                                .inLine(12))
                        .by(accessFrom(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(Report.class, "isEmpty")
                                .inLine(13))
                        .from("report")
                        .by(accessFrom(ReportService.class, "getReport", String.class)
                                .toMethod(ImportService.class, "process", String.class)
                                .inLine(10))
                        .from("importer")
                        .by(accessFrom(ImportService.class, "process", String.class)
                                .toMethod(AdministrationService.class, "createCustomerId", String.class)
                                .inLine(11)))

                .ofRule("slices matching '..(complexcycles).(*)..' should be free of cycles")
                .by(cycleFromComplexSlice1To2())
                .by(cycleFromComplexSlice1To2To3To5())
                .by(cycle().from("slice1 of complexcycles")
                        .by(accessFrom(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .by(accessFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .toConstructor(ClassWithAccessedFieldCallingMethodInSliceOne.class)
                                .inLine(8))
                        .by(accessFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .setting().field(ClassWithAccessedFieldCallingMethodInSliceOne.class, "accessedField")
                                .inLine(8))
                        .from("slice4 of complexcycles")
                        .by(accessFrom(ClassWithAccessedFieldCallingMethodInSliceOne.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(10)))
                .by(cycle().from("slice1 of complexcycles")
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceThree")
                                .toMethod(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .inLine(14))
                        .from("slice3 of complexcycles")
                        .by(accessFrom(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .toConstructor(InstantiatedClassInSliceFive.class)
                                .inLine(7))
                        .from("slice5 of complexcycles")
                        .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toConstructor(ClassBeingCalledInSliceOne.class)
                                .inLine(7))
                        .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(7)))

                .ofRule("Slices of complex scenario ignoring some violations should be free of cycles")
                .by(cycleFromComplexSlice1To2())
                .by(cycleFromComplexSlice1To2To3To5())

                .toDynamicTests();
    }

    private static CyclicErrorMatcher cycleFromComplexSlice1To2() {
        return cycle()
                .from("slice1 of complexcycles")
                .by(accessFrom(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                        .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                        .inLine(9))
                .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                        .toConstructor(InstantiatedClassInSliceTwo.class)
                        .inLine(10))
                .from("slice2 of complexcycles")
                .by(inheritanceFrom(SliceTwoInheritingFromSliceOne.class)
                        .extending(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class))
                .by(accessFrom(SliceTwoInheritingFromSliceOne.class, CONSTRUCTOR_NAME)
                        .toConstructor(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class)
                        .inLine(5))
                .by(accessFrom(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                        .toMethod(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                        .inLine(9));
    }

    private static CyclicErrorMatcher cycleFromComplexSlice1To2To3To5() {
        return cycle().from("slice1 of complexcycles")
                .by(accessFrom(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                        .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                        .inLine(9))
                .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                        .toConstructor(InstantiatedClassInSliceTwo.class)
                        .inLine(10))
                .from("slice2 of complexcycles")
                .by(inheritanceFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class)
                        .extending(InheritedClassInSliceThree.class))
                .by(accessFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, CONSTRUCTOR_NAME)
                        .toConstructor(InheritedClassInSliceThree.class)
                        .inLine(6))
                .from("slice3 of complexcycles")
                .by(accessFrom(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                        .toConstructor(InstantiatedClassInSliceFive.class)
                        .inLine(7))
                .from("slice5 of complexcycles")
                .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                        .toConstructor(ClassBeingCalledInSliceOne.class)
                        .inLine(7))
                .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                        .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                        .inLine(7));
    }

    @TestFactory
    Stream<DynamicTest> DaoRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.DaoRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.DaoRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.DaoRulesTest.class)

                .ofRule("DAOs should reside in a package '..dao..'")
                .by(javaPackageOf(InWrongPackageDao.class).notMatching("..dao.."))

                .ofRule("Entities should reside in a package '..domain..'")
                .by(javaPackageOf(EntityInWrongPackage.class).notMatching("..domain.."))

                .ofRule("Only DAOs may use the " + EntityManager.class.getSimpleName())
                .by(accessFrom(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(EntityManager.class, "persist", Object.class)
                        .inLine(26))
                .by(accessFrom(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(ServiceViolatingDaoRules.MyEntityManager.class, "persist", Object.class)
                        .inLine(27))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> InterfaceRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.InterfaceRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.InterfaceRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.InterfaceRulesTest.class)

                .ofRule("no classes that are interfaces should have name matching '.*Interface'")
                .by(clazz(SomeBusinessInterface.class).havingNameMatching(".*Interface"))

                .ofRule("no classes that are interfaces should have simple name containing 'Interface'")
                .by(clazz(SomeBusinessInterface.class).havingSimpleNameContaining("Interface"))
                .by(clazz(SomeInterfacePlacedInTheWrongPackage.class).havingSimpleNameContaining("Interface"))

                .ofRule("no classes that reside in a package '..impl..' should be interfaces")
                .by(clazz(SomeInterfacePlacedInTheWrongPackage.class).beingAnInterface())

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> LayerDependencyRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.LayerDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.LayerDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.LayerDependencyRulesTest.class)

                .ofRule("no classes that reside in a package '..service..' " +
                        "should access classes that reside in a package '..controller..'")
                .by(accessFrom(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .getting().field(UseCaseOneTwoController.class, someString)
                        .inLine(13))
                .by(callFrom(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(14))
                .by(callFrom(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toMethod(UseCaseTwoController.class, doSomethingTwo)
                        .inLine(15))

                .ofRule("no classes that reside in a package '..persistence..' should " +
                        "access classes that reside in a package '..service..'")
                .by(callFrom(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(14))

                .ofRule("classes that reside in a package '..service..' should " +
                        "only be accessed by any package ['..controller..', '..service..']")
                .by(callFrom(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(14))
                .by(callFrom(SomeMediator.class, violateLayerRulesIndirectly)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(15))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> LayeredArchitectureTest() {
        BiConsumer<String, ExpectedTestFailures> addExpectedCommonFailure =
                (memberName, expectedTestFailures) ->
                        expectedTestFailures
                                .ofRule(memberName,
                                        "Layered architecture consisting of" + lineSeparator() +
                                                "layer 'Controllers' ('com.tngtech.archunit.example.controller..')" + lineSeparator() +
                                                "layer 'Services' ('com.tngtech.archunit.example.service..')" + lineSeparator() +
                                                "layer 'Persistence' ('com.tngtech.archunit.example.persistence..')" + lineSeparator() +
                                                "where layer 'Controllers' may not be accessed by any layer" + lineSeparator() +
                                                "where layer 'Services' may only be accessed by layers ['Controllers']" + lineSeparator() +
                                                "where layer 'Persistence' may only be accessed by layers ['Services']")

                                .by(inheritanceFrom(DaoCallingService.class)
                                        .implementing(ServiceInterface.class))

                                .by(callFrom(DaoCallingService.class, "violateLayerRules")
                                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                                        .inLine(14)
                                        .asDependency())

                                .by(callFrom(ServiceViolatingLayerRules.class, "illegalAccessToController")
                                        .toConstructor(UseCaseTwoController.class)
                                        .inLine(14)
                                        .asDependency())

                                .by(callFrom(ServiceViolatingLayerRules.class, "illegalAccessToController")
                                        .toMethod(UseCaseTwoController.class, "doSomethingTwo")
                                        .inLine(15)
                                        .asDependency())

                                .by(accessFrom(ServiceViolatingLayerRules.class, "illegalAccessToController")
                                        .getting().field(UseCaseOneTwoController.class, "someString")
                                        .inLine(13)
                                        .asDependency());

        ExpectedTestFailures expectedTestFailures = ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.LayeredArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit4.LayeredArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit5.LayeredArchitectureTest.class);

        addExpectedCommonFailure.accept("layer_dependencies_are_respected", expectedTestFailures);
        expectedTestFailures
                .by(callFrom(SomeMediator.class, "violateLayerRulesIndirectly")
                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                        .inLine(15)
                        .asDependency());

        addExpectedCommonFailure.accept("layer_dependencies_are_respected_with_exception", expectedTestFailures);

        return expectedTestFailures.toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> MethodReturnTypeTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.MethodReturnTypeTest.class,
                        com.tngtech.archunit.exampletest.junit4.MethodReturnTypeTest.class,
                        com.tngtech.archunit.exampletest.junit5.MethodReturnTypeTest.class)

                .ofRule("methods that reside in a package '..anticorruption..' and are public "
                        + String.format("should return type %s, ", WrappedResult.class.getName())
                        + "because we don't want to couple the client code directly to the return types of the encapsulated module")
                .by(method(WithIllegalReturnType.class, "directlyReturnInternalType").returningType(InternalType.class))
                .by(method(WithIllegalReturnType.class, "otherIllegalMethod", String.class).returningType(int.class))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> NamingConventionTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.NamingConventionTest.class,
                        com.tngtech.archunit.exampletest.junit4.NamingConventionTest.class,
                        com.tngtech.archunit.exampletest.junit5.NamingConventionTest.class)

                .ofRule(String.format(
                        "classes that reside in a package '..service..' "
                                + "and are annotated with @%s "
                                + "should have simple name starting with 'Service'",
                        MyService.class.getSimpleName()))
                .by(simpleNameOf(WronglyNamedSvc.class).notStartingWith("Service"))

                .ofRule("classes that reside in a package '..controller..' should have simple name not containing 'Gui'")
                .by(simpleNameOf(SomeGuiController.class).containing("Gui"))

                .ofRule(String.format(
                        "classes that reside in a package '..controller..' "
                                + "or are annotated with @%s "
                                + "or are assignable to %s "
                                + "should have simple name ending with 'Controller'",
                        MyController.class.getSimpleName(), AbstractController.class.getName()))
                .by(simpleNameOf(InheritedControllerImpl.class).notEndingWith("Controller"))
                .by(simpleNameOf(SomeUtility.class).notEndingWith("Controller"))
                .by(simpleNameOf(WronglyAnnotated.class).notEndingWith("Controller"))

                .ofRule("classes that have simple name containing 'Controller' should reside in a package '..controller..'")
                .by(javaClass(AbstractController.class).notResidingIn("..controller.."))
                .by(javaClass(AnnotatedController.class).notResidingIn("..controller.."))
                .by(javaClass(InheritedControllerImpl.class).notResidingIn("..controller.."))
                .by(javaClass(MyController.class).notResidingIn("..controller.."))

                .toDynamicTests();
    }

    // FIXME: This can at the moment not really be covered by JUnit support, but probably should be...
    @TestFactory
    Stream<DynamicTest> SecurityTest() {
        ExpectedTestFailures expectedTestFailures = ExpectedTestFailures
                .forTests(SecurityTest.class);

        Consumer<String> addExpectedFailure = ruleText -> expectedTestFailures.ofRule(ruleText)
                .by(callFrom(WrongSecurityCheck.class, "doCustomNonsense")
                        .toMethod(CertificateFactory.class, "getInstance", String.class)
                        .inLine(19))
                .by(callFrom(WrongSecurityCheck.class, "doCustomNonsense")
                        .toMethod(CertificateFactory.class, "generateCertificate", InputStream.class)
                        .inLine(19));

        addExpectedFailure.accept("classes that reside in a package 'java.security..' "
                + "should only be accessed by any package ['..example.security..', 'java.security..'], "
                + "because we want to have one isolated cross-cutting concern 'security'");

        addExpectedFailure.accept("classes that reside in a package 'java.security.cert..' "
                + "should only be accessed by any package ['..example.security..', 'java..', '..sun..', 'javax..', 'apple.security..']");

        return expectedTestFailures.toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> SessionBeanRulesTest() {
        MessageAssertionChain.Link someBusinessInterfaceIsImplementedByTwoBeans =
                new MessageAssertionChain.Link() {
                    @Override
                    public Result filterMatching(List<String> lines) {
                        Collection<String> interesting = filter(lines, containsPattern(" is implemented by "));
                        if (interesting.size() != 1) {
                            return new Result(false, lines);
                        }
                        String[] parts = interesting.iterator().next().split(" is implemented by ");
                        if (parts.length != 2) {
                            return new Result(false, lines);
                        }

                        if (partsMatchExpectedViolation(parts)) {
                            List<String> resultLines = new ArrayList<>(lines);
                            resultLines.removeAll(interesting);
                            return new Result(true, resultLines);
                        } else {
                            return new Result(false, lines);
                        }
                    }

                    private boolean partsMatchExpectedViolation(String[] parts) {
                        ImmutableSet<String> violations = ImmutableSet.copyOf(parts[1].split(", "));
                        return parts[0].equals(SomeBusinessInterface.class.getSimpleName()) &&
                                violations.equals(ImmutableSet.of(
                                        ClassViolatingSessionBeanRules.class.getSimpleName(),
                                        SecondBeanImplementingSomeBusinessInterface.class.getSimpleName()));
                    }

                    @Override
                    public String getDescription() {
                        String violatingImplementations = Joiner.on(", ").join(
                                ClassViolatingSessionBeanRules.class.getSimpleName(),
                                SecondBeanImplementingSomeBusinessInterface.class.getSimpleName());

                        return String.format("Message contains: %s is implemented by {%s}",
                                SomeBusinessInterface.class.getSimpleName(), violatingImplementations);
                    }
                };

        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.SessionBeanRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.SessionBeanRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.SessionBeanRulesTest.class)

                .ofRule("No Stateless Session Bean should have state")
                .by(accessFrom(ClassViolatingSessionBeanRules.class, "setState", String.class)
                        .setting().field(ClassViolatingSessionBeanRules.class, "state")
                        .inLine(25))
                .by(accessFrom(OtherClassViolatingSessionBeanRules.class, init)
                        .setting().field(ClassViolatingSessionBeanRules.class, "state")
                        .inLine(13))

                .ofRule("classes that are business interfaces should have an unique implementation")
                .by(someBusinessInterfaceIsImplementedByTwoBeans)

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> SlicesIsolationTest() {
        BiConsumer<String, ExpectedTestFailures> addExpectedCommonFailureFor_controllers_should_only_use_their_own_slice =
                (memberName, expectedTestFailures) ->
                        expectedTestFailures
                                .ofRule(memberName, "Controllers should not depend on each other")
                                .by(sliceDependency()
                                        .described("Controller one calls Controller three")
                                        .by(accessFrom(UseCaseOneThreeController.class, doSomethingOne)
                                                .toConstructor(UseCaseThreeController.class)
                                                .inLine(9))
                                        .by(accessFrom(UseCaseOneThreeController.class, doSomethingOne)
                                                .toMethod(UseCaseThreeController.class, doSomethingThree)
                                                .inLine(9)))
                                .by(sliceDependency()
                                        .described("Controller two calls Controller one")
                                        .by(accessFrom(UseCaseTwoController.class, doSomethingTwo)
                                                .toConstructor(UseCaseOneTwoController.class)
                                                .inLine(9))
                                        .by(accessFrom(UseCaseTwoController.class, doSomethingTwo)
                                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                                .inLine(9)));

        ExpectedTestFailures expectedTestFailures = ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.SlicesIsolationTest.class,
                        com.tngtech.archunit.exampletest.junit4.SlicesIsolationTest.class,
                        com.tngtech.archunit.exampletest.junit5.SlicesIsolationTest.class);

        // controllers_should_only_use_their_own_slice
        addExpectedCommonFailureFor_controllers_should_only_use_their_own_slice
                .accept("controllers_should_only_use_their_own_slice", expectedTestFailures);
        expectedTestFailures.by(sliceDependency()
                .described("Controller one calls Controller two")
                .by(accessFrom(UseCaseOneTwoController.class, doSomethingOne)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(10))
                .by(accessFrom(UseCaseOneTwoController.class, doSomethingOne)
                        .toMethod(UseCaseTwoController.class, doSomethingTwo)
                        .inLine(10)))
                .by(sliceDependency()
                        .described("Controller three calls Controller one")
                        .by(accessFrom(UseCaseThreeController.class, doSomethingThree)
                                .toConstructor(UseCaseOneTwoController.class)
                                .inLine(9))
                        .by(accessFrom(UseCaseThreeController.class, doSomethingThree)
                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .inLine(9)));

        // controllers_should_only_use_their_own_slice_with_custom_ignore
        addExpectedCommonFailureFor_controllers_should_only_use_their_own_slice
                .accept("controllers_should_only_use_their_own_slice_with_custom_ignore", expectedTestFailures);

        // specific_controllers_should_only_use_their_own_slice
        expectedTestFailures
                .ofRule("Controllers one and two should not depend on each other")
                .by(sliceDependency()
                        .described("Controller one calls Controller two")
                        .by(accessFrom(UseCaseOneTwoController.class, doSomethingOne)
                                .toConstructor(UseCaseTwoController.class)
                                .inLine(10))
                        .by(accessFrom(UseCaseOneTwoController.class, doSomethingOne)
                                .toMethod(UseCaseTwoController.class, doSomethingTwo)
                                .inLine(10)))
                .by(sliceDependency()
                        .described("Controller two calls Controller one")
                        .by(accessFrom(UseCaseTwoController.class, doSomethingTwo)
                                .toConstructor(UseCaseOneTwoController.class)
                                .inLine(9))
                        .by(accessFrom(UseCaseTwoController.class, doSomethingTwo)
                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .inLine(9)));

        return expectedTestFailures.toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> ThirdPartyRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.ThirdPartyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.ThirdPartyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.ThirdPartyRulesTest.class)

                .ofRule(String.format("classes should not instantiate %s and its subclasses, but instead use %s",
                        ThirdPartyClassWithProblem.class.getSimpleName(),
                        ThirdPartyClassWorkaroundFactory.class.getSimpleName()))
                .by(callFrom(ClassViolatingThirdPartyRules.class, "illegallyInstantiateThirdPartyClass")
                        .toConstructor(ThirdPartyClassWithProblem.class)
                        .inLine(9))
                .by(callFrom(ClassViolatingThirdPartyRules.class, "illegallyInstantiateThirdPartySubClass")
                        .toConstructor(ThirdPartySubClassWithProblem.class)
                        .inLine(17))

                .toDynamicTests();
    }
}
