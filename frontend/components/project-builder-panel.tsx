"use client";

import { useState } from "react";

type ProjectPhase = {
  title: string;
  outcome: string;
  tasks: string[];
};

type ProjectPlan = {
  provider: string;
  model: string;
  title: string;
  objective: string;
  stack: string[];
  phases: ProjectPhase[];
  stretchGoals: string[];
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function ProjectBuilderPanel({
  accountId,
  disabled,
}: {
  accountId: string;
  disabled: boolean;
}) {
  const [goal, setGoal] = useState("");
  const [plan, setPlan] = useState<ProjectPlan | null>(null);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);

  async function generatePlan() {
    setError("");
    setStatus("Asking OpenAI to design a project plan...");
    setIsGenerating(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/accounts/${encodeURIComponent(
          accountId
        )}/learning/project-plan`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ goal }),
        }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(body?.message || body?.error || "Project plan failed.");
      }

      setPlan(body as ProjectPlan);
      setStatus(`Generated with ${body.provider}: ${body.model}.`);
    } catch (exception) {
      setPlan(null);
      setError(
        exception instanceof Error ? exception.message : "Project plan failed."
      );
      setStatus("");
    } finally {
      setIsGenerating(false);
    }
  }

  return (
    <div className="learning-card">
      <div className="stack">
        <p className="eyebrow">Project Builder</p>
        <h3 className="section-title">Design a strong portfolio project.</h3>
        <p className="body-copy">
          Describe what you want to build, or leave it blank and let AI
          choose a practical project with the best stack, phases, and scope.
        </p>
      </div>

      <textarea
        className="text-area"
        value={goal}
        onChange={(event) => setGoal(event.target.value)}
        disabled={disabled || isGenerating}
        placeholder="Example: Build a Spring Boot API with PostgreSQL, Redis, auth, and AI chat."
      />
      <button
        className={`button ${disabled ? "button-muted" : "button-accent"}`}
        type="button"
        onClick={generatePlan}
        disabled={disabled || isGenerating}
      >
        {isGenerating ? "Building plan..." : "Generate project plan"}
      </button>

      {status ? <p className="status-text">{status}</p> : null}
      {error ? <p className="status-warning">{error}</p> : null}

      {plan ? (
        <div className="project-plan">
          <div className="learning-wide-result">
            <p className="detail-label">Project</p>
            <h4>{plan.title}</h4>
            <p className="body-copy">{plan.objective}</p>
            <div className="topic-video-links">
              {plan.stack.map((item) => (
                <span className="surface-chip" key={item}>
                  {item}
                </span>
              ))}
            </div>
          </div>

          {plan.phases.map((phase, index) => (
            <div className="project-phase" key={`${phase.title}-${index}`}>
              <p className="eyebrow">Phase {index + 1}</p>
              <h4>{phase.title}</h4>
              <p className="status-text">{phase.outcome}</p>
              <ul className="formatted-list">
                {phase.tasks.map((task) => (
                  <li key={task}>{task}</li>
                ))}
              </ul>
            </div>
          ))}

          {plan.stretchGoals.length > 0 ? (
            <div className="learning-wide-result">
              <p className="detail-label">Stretch goals</p>
              <ul className="formatted-list">
                {plan.stretchGoals.map((goalItem) => (
                  <li key={goalItem}>{goalItem}</li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
