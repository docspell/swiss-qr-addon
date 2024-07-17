{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    devshell-tools.url = "github:eikek/devshell-tools";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    devshell-tools,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
      ciPkgs = with pkgs; [
        devshell-tools.packages.${system}.sbt17
        jdk17
      ];
      devshellPkgs =
        ciPkgs
        ++ (with pkgs; [
          jq
          scala-cli
        ]);
    in {
      formatter = pkgs.alejandra;

      devShells = {
        default = pkgs.mkShellNoCC {
          buildInputs = devshellPkgs;

          SBT_OPTS = "-Xmx2G -Xss4m";
        };
        ci = pkgs.mkShellNoCC {
          buildInputs = ciPkgs;
          SBT_OPTS = "-Xmx2G -Xss4m";
        };
      };
    });
}
