prog= timer_irq_sim timer_irq software_irq exception

ifndef MARCH
MARCH:=rv32i
endif

all: $(prog)
	

define make-test
$1: $1.s
	riscv32-unknown-elf-as $1.s -o $1.o -march=$(MARCH)
	riscv32-unknown-elf-ld $1.o -o $1.om
	riscv32-unknown-elf-objcopy -O binary $1.om $1.bin
	rm $1.o $1.om
endef

$(foreach test-program,$(prog),$(eval $(call make-test,$(test-program))))
