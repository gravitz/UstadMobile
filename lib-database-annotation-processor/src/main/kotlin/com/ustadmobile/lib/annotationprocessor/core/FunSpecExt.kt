package com.ustadmobile.lib.annotationprocessor.core

import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Insert
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement

/**
 * Simple shorthand function to check if the given function spec
 * contains the given annotation
 */
fun <A: Annotation> FunSpec.hasAnnotation(annotationClass: Class<A>) : Boolean {
    return annotations.any { it.className == annotationClass.asClassName() }
}

/**
 * Simple shorthand to see if the given FunSpec has any of the annotations from the vararg params
 */
fun <A : Annotation> FunSpec.hasAnyAnnotation(vararg annotationsClasses: Class<out A>) : Boolean {
    val annotationClassNames = annotationsClasses.map { it.asClassName() }
    return annotations.any { annotationSpec ->
        annotationClassNames.any { it == annotationSpec.className }
    }
}

/**
 * Where this function represents a DAO function with a query, get the query SQL
 */
fun FunSpec.daoQuerySql() = annotations.daoQuerySql()

/**
 * Determines if this FunSpec represents a DAO query function that where the return result
 * includes syncable entities
 */
fun FunSpec.isQueryWithSyncableResults(processingEnv: ProcessingEnvironment) =
        hasAnnotation(Query::class.java) &&
        returnType?.hasSyncableEntities(processingEnv) == true

/**
 * Gets a list of the syncable entities that are used in the given FunSpec where this is a DAO
 * function.
 * For functions annotated with Query, this will return any syncable entities that are in the
 * return result of a select query,
 */
fun FunSpec.daoFunSyncableEntityTypes(processingEnv: ProcessingEnvironment,
                                      allKnownEntityTypesMap: Map<String, TypeElement>) : List<ClassName>{
    if(hasAnnotation(Query::class.java)) {
        val querySql = daoQuerySql()
        if(querySql.isSQLAModifyingQuery()) {
            val modifiedEntity = findEntityModifiedByQuery(querySql,
                    allKnownEntityTypesMap.keys.toList())
            if(modifiedEntity != null) {
                val modifiedTypeEl = allKnownEntityTypesMap[modifiedEntity]
                        ?: throw IllegalArgumentException("${this.name} is modifying an unknown entity!")
                return modifiedTypeEl.asClassName().syncableEntities(processingEnv)
            }else {
                return listOf()
            }
        }else {
            return returnType?.unwrapQueryResultComponentType()?.syncableEntities(processingEnv) ?: listOf()
        }
    }else if(hasAnyAnnotation(Delete::class.java, Update::class.java, Insert::class.java)) {
        return parameters[0].type.unwrapListOrArrayComponentType().syncableEntities(processingEnv)
    }else {
        return listOf()
    }
}
