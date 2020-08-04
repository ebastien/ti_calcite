package com.amadeus.ti.calcite

import org.apache.calcite.sql.SqlFunctionCategory
import org.apache.calcite.sql.SqlAggFunction
import org.apache.calcite.sql.SqlKind
import org.apache.calcite.sql.`type`.OperandTypes
import org.apache.calcite.sql.`type`.ReturnTypes
import org.apache.calcite.sql.`type`.SqlTypeName
import org.apache.calcite.util.Optionality

/**
 * Definition of the "GROUP_CONCAT" SQL function.
 */
class SqlGroupConcatFunction extends SqlAggFunction(
  "GROUP_CONCAT",
  null,
  SqlKind.OTHER_FUNCTION,
  ReturnTypes.explicit(SqlTypeName.VARCHAR),
  null,
  OperandTypes.or(
    OperandTypes.sequence(
      "'GROUP_CONCAT(<VARCHAR>)'\n",
      OperandTypes.STRING
    ),
    OperandTypes.sequence(
      "'GROUP_CONCAT(<VARCHAR>, <VARCHAR>)'\n",
      OperandTypes.STRING,
      OperandTypes.STRING
    )
  ),
  SqlFunctionCategory.STRING,
  false,
  false, 
  Optionality.OPTIONAL) {
  //~ Methods ----------------------------------------------------------------

}

// End SqlGroupConcatFunction.java
