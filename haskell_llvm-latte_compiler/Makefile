SRCDIR = src
GENDIR = src_generated
BINDIR = bin
DOCDIR = docs
LIBDIR = lib

all: clean release copyBinary

release: $(BINDIR) $(GENDIR)
	ghc -i$(SRCDIR) -i$(GENDIR) --make -O2 -Wno-tabs $(SRCDIR)/Main.hs -outputdir $(BINDIR) -o $(BINDIR)/Main
	cp $(BINDIR)/Main $(BINDIR)/latc_llvm
	$(RM) $(BINDIR)/$(LIBDIR)
	ln -s ../$(LIBDIR) $(BINDIR)/$(LIBDIR)

debug: $(BINDIR) $(GENDIR)
	ghc -i$(SRCDIR) -i$(GENDIR) --make -W -Wcompat -Wno-tabs $(SRCDIR)/Main.hs -outputdir $(BINDIR) -o $(BINDIR)/Main
	cp $(BINDIR)/Main $(BINDIR)/latc_llvm
	$(RM) $(BINDIR)/$(LIBDIR)
	ln -s ../$(LIBDIR) $(BINDIR)/$(LIBDIR)

exampleProgram: $(BINDIR) $(GENDIR)
	ghc -i$(SRCDIR) -i$(GENDIR) --make -O2 -Wno-tabs $(GENDIR)/TestLatte.hs -outputdir $(BINDIR) -o $(BINDIR)/TestLatte

copyBinary:
	cp $(BINDIR)/latc_llvm ./latc_llvm

clean:
	$(RM) -r $(BINDIR)
	$(RM) -r latc_llvm

$(BINDIR):
	mkdir -p $(BINDIR)

$(GENDIR):
	mkdir -p $(GENDIR)

.PHONY: all clean copyBinary debug exampleProgram release
