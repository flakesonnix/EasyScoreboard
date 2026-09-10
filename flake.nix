{
  description = "EasyScoreboard — easy config scoreboard + DB";
  # relaxed sandbox for gradle network fetch in `nix build` (allowUnfree handled via import config)
  nixConfig = {
    sandbox = "relaxed";
  };

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    # nixpkgs 26.11 dropped x86_64-darwin; use only supported systems (keep aarch64-darwin for Apple Silicon)
    flake-utils.lib.eachSystem [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ] (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };
        # JDK 21 — Paper 1.26.2 requires Java 21
        jdk = pkgs.jdk21;
        gradle = pkgs.gradle_8;
        # jetbrains.idea — as requested: pkgs.jetbrains.idea (fallback to idea-community) — unfree → allowUnfree=true
        ideaPkg = if pkgs.jetbrains ? idea then pkgs.jetbrains.idea else pkgs.jetbrains.idea-community;
        # formatters — native where possible (nixfmt Rust, ktlint Kotlin-native if available)
        nixFmt = pkgs.nixfmt; # native (was nixfmt-rfc-style)
        ktFmtCheck = pkgs.ktlint; # native wrapper, prefer `gradle spotlessApply` for project fmt
      in
      {
        devShells.default = pkgs.mkShell {
          name = "easyscoreboard";
          buildInputs = [
            jdk
            gradle
            pkgs.git
            pkgs.bash
            ideaPkg
            nixFmt
            ktFmtCheck
          ];

          shellHook = ''
            export JAVA_HOME=${jdk}
            echo "EasyScoreboard — java $(java -version 2>&1 | head -n1) | gradle $(gradle --version | grep Gradle)"
            echo "  gradle shadowJar        → build/libs/easyscoreboard-1.0.0.jar (Kotlin-JVM, Paper API)"
            echo "  gradle spotlessApply    → Kotlin fmt (ktlint 1.5.0 native)"
            echo "  gradle spotlessCheck    → Kotlin fmt check"
            echo "  nix fmt                 → Nix fmt (nixfmt native)"
            echo "  gradle idea             → generate JetBrains .idea/.iml"
            echo "  IDEA: nix develop -c idea . &  |  nix run .#idea (jetbrains.idea native)"
          '';
        };

        # `nix build` → plugin jar (gradle build without wrapper)
        # Gradle needs network to fetch plugins/deps → allow impure/noChroot (template convenience)
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "easyscoreboard";
          version = "1.0.0";
          src = ./.;
          nativeBuildInputs = [
            jdk
            gradle
            pkgs.cacert
          ];
          __noChroot = true;
          # __impure = true; # uncomment if nix >=2.18 requires explicit impure for network
          buildPhase = ''
            export GRADLE_USER_HOME=$TMPDIR/.gradle
            export HOME=$TMPDIR
            gradle --no-daemon -x test build
          '';
          installPhase = ''
            mkdir -p $out
            cp build/libs/*.jar $out/ 2>/dev/null || cp -r build $out/
          '';
        };

        # `nix run .#idea` → JetBrains IDEA (jetbrains.idea)
        packages.idea = ideaPkg;

        # `nix fmt` → format all Nix files (flake.nix etc.)
        formatter = nixFmt;
      }
    );
}
