package hevs.especial.utils

/**
 * Various settings for the project, especially to modify the pipeline behaviour.
 * Enable or disable options for the compiler path. Set paths for external tools.
 */
object Settings {

  /* DOT */

  /** Generate the DOT diagram or PDF file corresponding to the program. */
  final var PIPELINE_RUN_DOT: Boolean = true

  final val PIPELINE_EXPORT_PDF: Boolean = true // PIPELINE_RUN_DOT must be enabled and DOT installed



  /* WARNINGS */

  /** Print warnings before generating the code from the DSL program. */
  final val PIPELINE_RUN_CODE_CHECKER: Boolean = true



  /* RESOLVER */

  /** Define the maximum number of passes. After that, the resolver will stop. */
  final val RESOLVER_MAX_PASSES: Int = 64


  /* CODE FORMATTER */

  /**
   * Format the generated C code using AStyle.
   * The original file is automatically renamed with a ".orig" extension.
   */
  final val PIPELINE_RUN_FORMATTER: Boolean = true

  /** Path to the AStyle binary file */
  final val ASTYLE_PATH = "./third_party/astyle/%s"


  /* CODE COMPILER */

  /** Path of the source file in the C project */
  final val PROJECT_SRC_FILE = "csrc/src/main.cpp"

  /** Path of the binary (elf) file */
  final val PROJECT_BINARY_FILE = "csrc/target-qemu/csrc.elf"


  /* QEMU */

  /** Path to the root folder of QEMU for STM32. Relative to the Scala project path. */
  final val PATH_QEMU_STM32 = "../../stm32/qemu_stm32"


  /* MONITOR */

  /** Port used by the Scala TCP Monitor Server to communicate with the QEMU client. */
  final val MONITOR_TCP_CMD_PORT = 14001
}