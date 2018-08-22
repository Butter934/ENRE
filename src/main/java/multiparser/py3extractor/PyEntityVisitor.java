package multiparser.py3extractor;

import multiparser.py3extractor.antlr4.Python3BaseVisitor;
import multiparser.py3extractor.antlr4.Python3Parser;

public class PyEntityVisitor extends Python3BaseVisitor<String> {
    private String fileFullPath;
    private PyProcessTask processTask = new PyProcessTask();
    private PyContextHelper contextHelper = new PyContextHelper();

    int moduleId = -1;
    int classId = -1;
    int functionId = -1;

    public PyEntityVisitor(String fileFullPath) {
        this.fileFullPath = fileFullPath;

        //the directory of this file is a package
        if(contextHelper.isInitFile(fileFullPath)) {
            //its parent is a package or none. after finishing all packages, we should set the parentId for each package
            //save into singlecollection.entities.
            int packageId = processTask.processPackage(fileFullPath);
        }
        //this file is a module
        else {
            //its parent is a package or none. after finishing all files, we should set the parentId for each module
            //save into singlecollection.entities.
            moduleId = processTask.processModule(fileFullPath);
        }
    }


    /**
     * save classEntity with parentId, baseClassNames
     * grammar: classdef: 'class' NAME ('(' (arglist)? ')')? ':' suite;
     * @param ctx
     * @return
     */
    @Override
    public String visitClassdef(Python3Parser.ClassdefContext ctx) {
        String str = "";

        String className = ctx.NAME().getText();
        String baseStrs = "";
        //baseClass
        if(ctx.arglist() != null) {
            baseStrs = visitArglist(ctx.arglist());
        }

        if(contextHelper.isOneComStmAtTopLevel(ctx) && moduleId != -1) {
            classId = processTask.processClass(moduleId, className, baseStrs);
        }
        else {
            //classId = processTask.processClass(blockId, className, baseStrs);
        }
        //visit class body
        if(ctx.suite() != null) {
            visitSuite(ctx.suite());
        }
        classId = -1;

        str += ("class " + className + "(" + baseStrs + ")");
        return str;
    }

    /**
     * grammar: funcdef: 'def' NAME parameters ('->' test)? ':' suite;
     * @param ctx
     * @return
     */
    @Override
    public String visitFuncdef(Python3Parser.FuncdefContext ctx) {
        String functionName = ctx.NAME().getText();
        String paraStrs = "";
        if(ctx.parameters() != null) {
            paraStrs = visitParameters(ctx.parameters());
        }

        //a top-level function
        if(contextHelper.isOneComStmAtTopLevel(ctx) && moduleId != -1 && classId == -1) {
            functionId = processTask.processFunction(moduleId, functionName, paraStrs);
        }
        // a class method, class static method, or instance method
        else if(classId != -1) {
            if(contextHelper.isInDecorator(ctx)) { // classMethod, classStaticMethod
                String decorations = visitDecorated((Python3Parser.DecoratedContext) ctx.parent);
                functionId = processTask.processClassMethod(decorations, classId, functionName, paraStrs);
            }
            else{ //instanceMethod
                functionId = processTask.processInstMethod(classId, functionName, paraStrs);
            }
        }

        if(ctx.suite() != null) {
            visitSuite(ctx.suite());
        }
        functionId = -1;

        String str = "";
        str += ("def" + functionName + paraStrs);
        return str;
    }


    /**
     * decorated: decorators (classdef | funcdef | async_funcdef);
     * @param ctx
     * @return
     */
    @Override
    public String visitDecorated(Python3Parser.DecoratedContext ctx) {
        String str = "";
        if(ctx != null && ctx.decorators() != null) {
            str =  visitDecorators(ctx.decorators());
        }
        return str;
    }

    /**
     * decorators: decorator+;
     * @param ctx
     * @return
     */
    @Override
    public String visitDecorators(Python3Parser.DecoratorsContext ctx) {
        String str = "";
        if(ctx != null) {
            return str;
        }
        if(ctx.decorator() != null && !ctx.decorator().isEmpty()) {
            str += visitDecorator(ctx.decorator(0));
            for (int i = 1; i < ctx.decorator().size(); i++) {
                str += ",";
                str += visitDecorator(ctx.decorator(i));
            }
        }
        return str;
    }

    /**
     * decorator: '@' dotted_name ( '(' (arglist)? ')' )? NEWLINE;
     * @param ctx
     * @return
     */
    @Override
    public String visitDecorator(Python3Parser.DecoratorContext ctx) {
        String str = "";
        if(ctx != null && ctx.dotted_name() != null) {
            str =  visitDotted_name(ctx.dotted_name());
        }
        return str;
    }

    /**
     * dotted_name: NAME ('.' NAME)*;
     * @param ctx
     * @return
     */
    @Override
    public String visitDotted_name(Python3Parser.Dotted_nameContext ctx) {
        String str = "";
        if(ctx != null && ctx.NAME() != null && !ctx.NAME().isEmpty()) {
            str += ctx.NAME(0).getText();
            for (int i = 1; i < ctx.NAME().size(); i++) {
                str += ".";
                str += ctx.NAME(i).getText();
            }
        }
        return str;
    }

    /**
     * classdef: 'class' NAME ('(' (arglist)? ')')? ':' suite;
     * funcdef: 'def' NAME parameters ('->' test)? ':' suite;
     * if_stmt, while_stmt, try_stmt,for_stmt....
     * @param ctx
     * @return
     */
    @Override
    public String visitSuite(Python3Parser.SuiteContext ctx) {
        /*if (ctx == null) {
            return str;
        }

        //process class suite
        if (contextHelper.isSuiteInClass(ctx)) {

        }
        //process func suite
        else if (contextHelper.isSuiteInFunc(ctx)) {
            if(processTask.isInitMethod(functionId)) {
                //init method, need to save the instance's variable

            }
            else { //regular function

            }

        }
        //process if,for,while, try suite..
        else{
        }
        */

        return super.visitSuite(ctx);
    }

    /**
     * grammar: arglist: argument (',' argument)*  (',')?;
     * @param ctx
     * @return
     */
    @Override
    public String visitArglist(Python3Parser.ArglistContext ctx) {
        String str = "";
        if(ctx.argument() != null && !ctx.argument().isEmpty()) {
            str += visitArgument(ctx.argument(0));
            for (int i = 1; i < ctx.argument().size(); i++) {
                str += ",";
                str += visitArgument(ctx.argument(i));
            }
        }
        return str;
    }

    /**
     * grammar: atom_expr: (AWAIT)? atom trailer*;
     * @param ctx
     * @return
     */
    @Override
    public String visitAtom_expr(Python3Parser.Atom_exprContext ctx) {
        String str = "";
        if(ctx.atom() != null) {
            str += visitAtom(ctx.atom());
        }
        if(ctx.trailer() != null && !ctx.trailer().isEmpty()) {
            for (Python3Parser.TrailerContext trailerContext : ctx.trailer()) {
                str += visitTrailer(trailerContext);
            }
        }
        return str;
    }

    /**
     * atom: ('(' (yield_expr|testlist_comp)? ')'
     * |'[' (testlist_comp)? ']'
     * | '{' (dictorsetmaker)? '}'
     * | NAME | NUMBER | STRING+ | '...' | 'None' | 'True' | 'False');
     * @param ctx
     * @return
     */
    @Override
    public String visitAtom(Python3Parser.AtomContext ctx) {
        String str = "";
        if(ctx.NAME() != null) {
            str += ctx.NAME().getText();
        }
        return str;
    }

    /**
     * trailer:
     '(' (arglist)? ')'         #arglisttrailer
     | '[' subscriptlist ']'    #subscriptlisttrailer
     | '.' NAME                 #attributetrailer
     ;
     * @param ctx
     * @return
     */
    @Override
    public String visitTrailer(Python3Parser.TrailerContext ctx) {
        String str = "";
        if(ctx.NAME() != null) {
            str += ".";
            str += ctx.NAME().getText();
        }
        return str;
    }


    /**
     * parameters: '(' (typedargslist)? ')';
     * @param ctx
     * @return
     */
    @Override
    public String visitParameters(Python3Parser.ParametersContext ctx) {
        String str = "(";
        if(ctx != null && ctx.typedargslist() != null) {
            str += visitTypedargslist(ctx.typedargslist());
        }
        str += ")";
        return str;
    }

    /**
     * //jwx:typedargslist is the argument list of a function
     typedargslist: (tfpdef ('=' test)? (',' tfpdef ('=' test)?)* (',' (
     '*' (tfpdef)? (',' tfpdef ('=' test)?)* (',' ('**' tfpdef (',')?)?)?
     | '**' tfpdef (',')?)?)?
     | '*' (tfpdef)? (',' tfpdef ('=' test)?)* (',' ('**' tfpdef (',')?)?)?
     | '**' tfpdef (',')?);
     tfpdef: NAME (':' test)?;
     * @param ctx
     * @return
     */
    @Override
    public String visitTypedargslist(Python3Parser.TypedargslistContext ctx) {
        String str = "";
        //ignore test's content. test is the default value of an argument in a function
        if(ctx != null && ctx.tfpdef() != null && !ctx.tfpdef().isEmpty()) {
            str += ctx.tfpdef(0).NAME().getText();
            for (int i = 1; i < ctx.tfpdef().size(); i++) {
                str += ",";
                str += ctx.tfpdef(i).NAME().getText();
            }
        }
        return str;
    }




}