// @/mock/mockSuggestion.ts
export interface Suggestion {
  junctionId: string
  lightIndex: number
  state: string
  junctionName: string
  fromEdge: string
  toEdge: string
  duration: number
}

export const suggestionList: Suggestion[] = [
  {
    junctionId: "cluster_59665162_8742497506",
    lightIndex: 0,
    state: "G",
    junctionName: "Nutley Lane",
    fromEdge: "Roebuck Road",
    toEdge: "Gledswood Drive",
    duration: 10
  },
  {
    junctionId: "2455689105",
    lightIndex: 1,
    state: "r",
    junctionName: "Merrion Road",
    fromEdge: "Merrion Road",
    toEdge: "Merrion Road",
    duration: 8
  },
  {
    junctionId: "50877750",
    lightIndex: 1,
    state: "r",
    junctionName: "Beach Hill Road-Clonskeagh Road",
    fromEdge: "Beach Hill Road",
    toEdge: "Clonskeagh Road",
    duration: 8
  }
]
